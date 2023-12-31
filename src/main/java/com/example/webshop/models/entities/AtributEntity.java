package com.example.webshop.models.entities;

import lombok.*;

import javax.persistence.*;
import java.util.List;

@Data
@Entity
@Table(name = "atribut", schema = "webshop_ip", catalog = "")
public class AtributEntity {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id")
    private Integer id;
    @Basic
    @Column(name = "naziv")
    private String naziv;
    @Basic
    @Column(name = "tip")
    private String tip;
    @ManyToOne
    @ToString.Exclude
    @JoinColumn(name = "kategorija_id", referencedColumnName = "id", nullable = false)
    private KategorijaEntity kategorija;
    @OneToMany(mappedBy = "atribut")
    @ToString.Exclude
    private List<ProizvodAtributEntity> proizvod_atribut;

}
