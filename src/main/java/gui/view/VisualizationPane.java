package gui.view;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import models.Product;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;

public class VisualizationPane extends Pane {
    private final Canvas canvas;
    private final List<DrawnProduct> drawn = new ArrayList<>();
    private final Map<Integer, Color> userColors = new HashMap<>();

    private final long animStart = System.nanoTime();

    private Consumer<Product> onClick;

    public VisualizationPane() {
        canvas = new Canvas(800, 500);
        getChildren().add(canvas);
        widthProperty().addListener((o, a, b) -> {
            canvas.setWidth(getWidth());
            redraw();
        });
        heightProperty().addListener((o, a, b) -> {
            canvas.setHeight(getHeight());
            redraw();
        });

        canvas.setOnMouseClicked(e -> {
            if (onClick == null) return;

            for (int i = drawn.size() - 1; i >= 0; i--) {
                DrawnProduct dp = drawn.get(i);
                double dx = e.getX() - dp.screenX;
                double dy = e.getY() - dp.screenY;
                if (Math.sqrt(dx*dx + dy*dy) <= dp.radius) {
                    onClick.accept(dp.product);
                    return;
                }
            }
        });

        AnimationTimer timer = new AnimationTimer() {
            @Override public void handle(long now) {
                redraw();
            }
        };
        timer.start();
    }

    public void setOnProductClicked(Consumer<Product> handler) { this.onClick = handler; }

    public void setProducts(List<Product> products) {
        long now = System.nanoTime();
        Map<Integer, DrawnProduct> existing = new HashMap<>();
        for (DrawnProduct d : drawn) if (d.product.getId() != null) existing.put(d.product.getId(), d);

        List<DrawnProduct> next = new ArrayList<>();
        for (Product p : products) {
            if (p.getId() == null) continue;
            DrawnProduct prev = existing.remove(p.getId());
            if (prev == null) {
                next.add(new DrawnProduct(p, now, now, 0));
            } else {
                long updated = prev.updatedAt;
                if (!sameContent(prev.product, p)) updated = now;
                DrawnProduct dp = new DrawnProduct(p, prev.bornAt, updated, prev.diedAt);
                next.add(dp);
            }
        }
        for (DrawnProduct d : existing.values()) {
            if (d.diedAt == 0) d.diedAt = now;
            if (now - d.diedAt < 600_000_000L) next.add(d);
        }
        drawn.clear();
        drawn.addAll(next);
    }

    private static boolean sameContent(Product a, Product b) {
        return a.getPrice() == b.getPrice()
                && a.getCoordinates() != null && b.getCoordinates() != null
                && a.getCoordinates().getX().equals(b.getCoordinates().getX())
                && a.getCoordinates().getY() == b.getCoordinates().getY()
                && java.util.Objects.equals(a.getName(), b.getName());
    }

    private Color colorFor(int userId) {
        return userColors.computeIfAbsent(userId, id -> {
            Random r = new Random((long) id * 0x9E3779B1L + 1);
            float hue = r.nextFloat() * 360f;
            return Color.hsb(hue, 0.65, 0.85);
        });
    }

    private void redraw() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        g.setFill(Color.web("#202830"));
        g.fillRect(0, 0, w, h);

        g.setStroke(Color.web("#2f3a44"));
        g.setLineWidth(1);
        double offsetX = 0;
        double cx = w / 2 + offsetX;
        double offsetY = 0;
        double cy = h / 2 + offsetY;
        for (int gx = 0; gx < w; gx += 40) g.strokeLine(gx, 0, gx, h);
        for (int gy = 0; gy < h; gy += 40) g.strokeLine(0, gy, w, gy);
        g.setStroke(Color.web("#7a8b99"));
        g.strokeLine(0, cy, w, cy);
        g.strokeLine(cx, 0, cx, h);

        long now = System.nanoTime();
        long elapsedTotal = now - animStart;

        for (DrawnProduct dp : drawn) {
            Product p = dp.product;
            if (p.getCoordinates() == null) continue;
            double scale = 1.0;
            double sx = cx + p.getCoordinates().getX() * scale * 0.15;
            double sy = cy - p.getCoordinates().getY() * scale * 0.5;
            dp.screenX = sx; dp.screenY = sy;

            double baseR = Math.max(8, Math.min(60, Math.sqrt(Math.max(1, p.getPrice())) * 1.5));

            // appear scale animation
            double appearT = Math.min(1.0, (now - dp.bornAt) / 400_000_000.0);
            double die = dp.diedAt == 0 ? 1.0 : Math.max(0.0, 1.0 - (now - dp.diedAt) / 600_000_000.0);

            // pulse: 0.92..1.08
            double pulse = 1.0 + 0.08 * Math.sin(elapsedTotal / 4.5e8);

            double r = baseR * appearT * die * pulse;
            dp.radius = r;

            g.setLineWidth(2);
            g.fillOval(sx - r, sy - r, 2*r, 2*r);
            g.strokeOval(sx - r, sy - r, 2*r, 2*r);

            g.setFill(Color.WHITE);
            g.setFont(Font.font(11));
            g.fillText("#" + p.getId() + " " + p.getName(), sx + r + 4, sy + 4);
        }
    }

    private static class DrawnProduct {
        Product product;
        long bornAt;
        long updatedAt;
        long diedAt;
        double screenX, screenY, radius;
        DrawnProduct(Product p, long born, long upd, long died) {
            this.product = p; this.bornAt = born; this.updatedAt = upd; this.diedAt = died;
        }
    }
}
