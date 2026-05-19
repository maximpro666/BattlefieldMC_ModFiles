package com.yourmod.teamsystem.core;

import java.util.ArrayList;
import java.util.List;

public class BorderZone {

    private String type = "rect";
    private double minX, minZ, maxX, maxZ;
    private List<double[]> polygon;

    public BorderZone() {}

    public static BorderZone rect(double minX, double minZ, double maxX, double maxZ) {
        BorderZone z = new BorderZone();
        z.type = "rect";
        z.minX = Math.min(minX, maxX);
        z.minZ = Math.min(minZ, maxZ);
        z.maxX = Math.max(minX, maxX);
        z.maxZ = Math.max(minZ, maxZ);
        return z;
    }

    public static BorderZone polygon(List<double[]> vertices) {
        BorderZone z = new BorderZone();
        z.type = "polygon";
        z.polygon = new ArrayList<>(vertices);
        return z;
    }

    public boolean contains(double x, double z) {
        if ("polygon".equals(type) && polygon != null && polygon.size() >= 3) {
            return pointInPolygon(x, z, polygon);
        }
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    public double distanceSq(double x, double z) {
        if (contains(x, z)) {
            if ("polygon".equals(type)) return 0;
            double dx = Math.min(Math.abs(x - minX), Math.abs(x - maxX));
            double dz = Math.min(Math.abs(z - minZ), Math.abs(z - maxZ));
            double d = Math.min(dx, dz);
            return d * d;
        }
        double[] closest = closestPoint(x, z);
        double dx = x - closest[0];
        double dz = z - closest[1];
        return dx * dx + dz * dz;
    }

    public double[] closestPoint(double x, double z) {
        if ("polygon".equals(type) && polygon != null && polygon.size() >= 3) {
            return closestPointOnPolygon(x, z, polygon);
        }
        double cx = clamp(x, minX, maxX);
        double cz = clamp(z, minZ, maxZ);
        return new double[]{cx, cz};
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getMinX() { return minX; }
    public void setMinX(double v) { minX = v; }
    public double getMinZ() { return minZ; }
    public void setMinZ(double v) { minZ = v; }
    public double getMaxX() { return maxX; }
    public void setMaxX(double v) { maxX = v; }
    public double getMaxZ() { return maxZ; }
    public void setMaxZ(double v) { maxZ = v; }

    public List<double[]> getPolygon() { return polygon; }
    public void setPolygon(List<double[]> polygon) { this.polygon = polygon; }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static boolean pointInPolygon(double x, double z, List<double[]> poly) {
        boolean inside = false;
        int n = poly.size();
        for (int i = 0, j = n - 1; i < n; j = i++) {
            double[] vi = poly.get(i);
            double[] vj = poly.get(j);
            if ((vi[1] > z) != (vj[1] > z) &&
                x < (vj[0] - vi[0]) * (z - vi[1]) / (vj[1] - vi[1]) + vi[0]) {
                inside = !inside;
            }
        }
        return inside;
    }

    private static double[] closestPointOnPolygon(double x, double z, List<double[]> poly) {
        int n = poly.size();
        double bestDist = Double.MAX_VALUE;
        double bestX = poly.get(0)[0], bestZ = poly.get(0)[1];
        for (int i = 0, j = n - 1; i < n; j = i++) {
            double[] a = poly.get(j);
            double[] b = poly.get(i);
            double[] p = closestPointOnSegment(x, z, a[0], a[1], b[0], b[1]);
            double dx = x - p[0];
            double dz = z - p[1];
            double d = dx * dx + dz * dz;
            if (d < bestDist) {
                bestDist = d;
                bestX = p[0];
                bestZ = p[1];
            }
        }
        return new double[]{bestX, bestZ};
    }

    private static double[] closestPointOnSegment(double px, double pz,
                                                   double ax, double az,
                                                   double bx, double bz) {
        double abx = bx - ax, abz = bz - az;
        double lenSq = abx * abx + abz * abz;
        if (lenSq == 0) return new double[]{ax, az};
        double t = ((px - ax) * abx + (pz - az) * abz) / lenSq;
        t = clamp(t, 0, 1);
        return new double[]{ax + t * abx, az + t * abz};
    }
}
