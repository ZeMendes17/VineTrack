package pt.ua.ies.vineTrack.entity;

import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "nutrient")
public class Nutrient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private Double Nitrogen;

    @Column(nullable = false)
    private Double Phosphorus;
    @Column(nullable = false)
    private Double Potassium;
    @Column(nullable = false)
    private Double Calcium;
    @Column(nullable = false)
    private Double Magnesium;
    @Column(nullable = false)
    private Double Chloride;

    @ManyToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "vine_id", referencedColumnName = "id")
    private List<Vine> vines;

    public Nutrient() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Double getNitrogen() {
        return Nitrogen;
    }

    public void setNitrogen(Double nitrogen) {
        Nitrogen = nitrogen;
    }

    public Double getPhosphorus() {
        return Phosphorus;
    }

    public void setPhosphorus(Double phosphorus) {
        Phosphorus = phosphorus;
    }

    public Double getPotassium() {
        return Potassium;
    }

    public void setPotassium(Double potassium) {
        Potassium = potassium;
    }

    public Double getCalcium() {
        return Calcium;
    }

    public void setCalcium(Double calcium) {
        Calcium = calcium;
    }

    public Double getMagnesium() {
        return Magnesium;
    }

    public void setMagnesium(Double magnesium) {
        Magnesium = magnesium;
    }

    public Double getChloride() {
        return Chloride;
    }

    public List<Vine> getVines() {
        return vines;
    }

    public void setVines(List<Vine> vines) {
        this.vines = vines;
    }

    public void setChloride(Double chloride) {
        Chloride = chloride;
    }
}
