package com.example.webshop.models.entities;

import lombok.*;

import javax.persistence.*;
import java.util.List;

@Data
@Entity
@Table(name = "kategorija", schema = "webshop_ip", catalog = "")
public class KategorijaEntity {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id")
    private Integer id;
    @Basic
    @Column(name = "naziv")
    private String naziv;
    @OneToMany(mappedBy = "kategorija")
    @ToString.Exclude
    private List<AtributEntity> atribut;
    @OneToMany(mappedBy = "kategorija")
    @ToString.Exclude
    private List<ProizvodEntity> proizvod;

}
