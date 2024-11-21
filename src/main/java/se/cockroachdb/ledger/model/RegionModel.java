package se.cockroachdb.ledger.model;

import java.util.List;
import java.util.Objects;

import org.springframework.hateoas.RepresentationModel;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"links", "embedded", "templates"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RegionModel extends RepresentationModel<RegionModel> {
    private String name;

    private List<CityModel> cityModels = List.of();

    private List<String> databaseRegions = List.of();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getDatabaseRegions() {
        return databaseRegions;
    }

    public void setDatabaseRegions(List<String> databaseRegions) {
        this.databaseRegions = databaseRegions;
    }

    public List<CityModel> getCityModels() {
        return cityModels;
    }

    public void setCityModels(List<CityModel> cityModels) {
        this.cityModels = cityModels;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        RegionModel that = (RegionModel) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name);
    }
}
