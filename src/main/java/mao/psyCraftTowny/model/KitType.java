package mao.psyCraftTowny.model;

public enum KitType {
    SWORDSMAN("Мечник"),
    ARCHER("Лучник"),
    ENGINEER("Инженер"),
    SUPPORT("Саппорт"),
    CROSSBOWMAN("Арбалетчик"),
    TANK("Танк"),
    NINJA("Ниндзя"),
    TRAPPER("Трапер"),
    MEDIC("Медик");

    private final String displayName;

    KitType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
