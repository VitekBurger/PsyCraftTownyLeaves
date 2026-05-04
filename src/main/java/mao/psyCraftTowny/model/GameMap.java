package mao.psyCraftTowny.model;

import org.bukkit.Location;

import java.util.Map;

public record GameMap(String code,
                      String displayName,
                      String menuItemKey,
                      Location worldBorderCenter,
                      double worldBorderSize,
                      Map<Integer, Location> teamSpawns,
                      Map<Integer, CapturePoint> capturePoints
) {
}
