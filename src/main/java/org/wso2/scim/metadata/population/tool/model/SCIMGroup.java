package org.wso2.scim.metadata.population.tool.model;

public class SCIMGroup {

    private String name;
    private String id;
    private String createdAt;
    private String updatedAt;

    private SCIMGroup() {}

    public SCIMGroup(String name, String id) {
        this.name = name;
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public String getId() {
        return this.id;
    }

    public String getCreatedAt() {
        return this.createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return this.updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String toString() {
        return "SCIMGroup{name='" + this.name + '\'' + ", id='" + this.id + '\'' + ", createdAt='" + this.createdAt +
                '\'' + ", updatedAt='" + this.updatedAt + '\'' + '}';
    }
}
