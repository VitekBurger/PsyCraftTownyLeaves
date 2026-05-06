package mao.psyCraftTowny.model;

import org.bukkit.Location;

import java.util.Map;

public record GameMap(String code,
                      String displayName,
                      String menuItemKey,
                      Location worldBorderCenter,
                      double worldBorderSize,
                      Mode mode,
                      Map<Integer, Location> teamSpawns,
                      Map<Integer, CapturePoint> capturePoints
) {

    public enum Mode {
        TDM(
                "Командный детматч",
                "Цель: убивать вражеских игроков, пока у них не закончатся респавны."
        ),
        CP(
                "Контрольные точки",
                "Цель: захватить все контрольные точки или убить всех вражеских игроков."
        ),
        FFA(
                "Каждый сам за себя",
                "Цель: совершить как можно больше убийств за 6 минут."
        );

        private final String ruName;
        private final String targetMessage;

        Mode(String ruName, String targetMessage) {
            this.ruName = ruName;
            this.targetMessage = targetMessage;
        }

        public String getRuName() {
            return ruName;
        }

        public String getTargetMessage() {
            return targetMessage;
        }
    }
}
