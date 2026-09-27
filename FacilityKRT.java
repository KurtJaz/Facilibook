package com.school.model;

public class FacilityKRT {
    public enum Type { CLASSROOM, LABORATORY, SPORTS, AUDITORIUM, LIBRARY, CONFERENCE }

    private String id;
    private String name;
    private Type type;
    private String location;
    private int capacity;
    private String description;
    private boolean active;

    public FacilityKRT() {}

    public FacilityKRT(String id, String name, Type type, String location, int capacity, String description) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.location = location;
        this.capacity = capacity;
        this.description = description;
        this.active = true;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public String toString() {
        return name + " [" + type + "] - " + location;
    }
}
