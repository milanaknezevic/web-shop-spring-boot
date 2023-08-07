package com.example.webshop.services.impl;

import com.example.webshop.exceptions.NotFoundException;
import com.example.webshop.models.dto.Comment;
import com.example.webshop.models.dto.JwtUser;
import com.example.webshop.models.dto.Product;
import com.example.webshop.models.dto.ProductAttribute;
import com.example.webshop.models.entities.*;
import com.example.webshop.models.requests.*;
import com.example.webshop.repositories.*;
import com.example.webshop.services.LogerService;
import com.example.webshop.services.ProductService;
import com.example.webshop.util.Util;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@Transactional
public class ProductImplService implements ProductService {

    private final ModelMapper modelMapper;
    private final CommentRepository commentRepository;
    private final ProductRepository productRepository;
    private final AtributeRepository atributeRepository;
    private final UserRepository userRepository;
    private final ImageRepository imageRepository;
    private final LogerService logerService;
    private final ProductAtributeRepository productAtributeRepository;
    @PersistenceContext
    private EntityManager entityManager;

    public ProductImplService(ModelMapper modelMapper, CommentRepository commentRepository, ProductRepository productRepository,
                              AtributeRepository atributeRepository, UserRepository userRepository, ImageRepository imageRepository, LogerService logerService, ProductAtributeRepository productAtributeRepository) {
        this.modelMapper = modelMapper;
        this.commentRepository = commentRepository;
        this.productRepository = productRepository;
        this.atributeRepository = atributeRepository;
        this.userRepository = userRepository;
        this.imageRepository = imageRepository;
        this.logerService = logerService;
        this.productAtributeRepository = productAtributeRepository;
    }

    @Override
    public Product findById(Integer id) throws NotFoundException {
        return modelMapper.map(productRepository.findById(id).orElseThrow(NotFoundException::new), Product.class);
    }

    @Override
    public Page<Product> getAllProducts(Pageable page) {
        return productRepository.findAllByZavrsenaPonuda(page).map(p -> modelMapper.map(p, Product.class));

    }

    @Override
    public Comment sendQuestion(Integer id, QuestionRequest questionRequest, Authentication authentication) {
        JwtUser user = (JwtUser) authentication.getPrincipal();
        KorisnikEntity korisnikEntity = userRepository.findById(user.getId()).orElseThrow(NotFoundException::new);
        ProizvodEntity proizvodEntity = productRepository.findById(id).orElseThrow(NotFoundException::new);
        KomentarEntity komentarEntity = modelMapper.map(questionRequest, KomentarEntity.class);
        komentarEntity.setId(null);
        komentarEntity.setKorisnik_komentar(korisnikEntity);
        komentarEntity.setProizvod_komentar(proizvodEntity);
        komentarEntity.setDatum(new Date());
        commentRepository.saveAndFlush(komentarEntity);
        entityManager.refresh(komentarEntity);
        logerService.insertLog("The user " + korisnikEntity.getKorisnickoIme() + " asked a question about the product " + komentarEntity.getProizvod_komentar().getNaslov(), this.getClass().getName());

        return modelMapper.map(komentarEntity, Comment.class);
    }

    @Override
    public Comment sendAnswer(Integer id, AnswerRequest answerRequest) {
        KomentarEntity komentarEntity = commentRepository.findById(id).orElseThrow(NotFoundException::new);
        komentarEntity.setOdgovor(answerRequest.getOdgovor());
        commentRepository.saveAndFlush(komentarEntity);
        entityManager.refresh(komentarEntity);
        logerService.insertLog("Product owner answered on question for product  " + komentarEntity.getProizvod_komentar().getNaslov(), this.getClass().getName());

        return modelMapper.map(komentarEntity, Comment.class);
    }

    @Override
    public Product purchaseProduct(Integer id, Authentication authentication) {
        ProizvodEntity proizvodEntity = productRepository.findById(id).orElseThrow(NotFoundException::new);
        JwtUser user = (JwtUser) authentication.getPrincipal();
        KorisnikEntity korisnikEntity = userRepository.findById(user.getId()).orElseThrow(NotFoundException::new);
        proizvodEntity.setZavrsenaPonuda(1);
        proizvodEntity.setKupac(korisnikEntity);
        productRepository.saveAndFlush(proizvodEntity);
        entityManager.refresh(proizvodEntity);
        logerService.insertLog("The user " + korisnikEntity.getKorisnickoIme() + " has purchased prdouct " + proizvodEntity.getNaslov(), this.getClass().getName());

        return modelMapper.map(proizvodEntity, Product.class);
    }

    @Override
    public Page<Product> searchProducts(Pageable page, SearchRequest searchRequest) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<ProizvodEntity> query = cb.createQuery(ProizvodEntity.class);
        Root<ProizvodEntity> root = query.from(ProizvodEntity.class);
        List<Predicate> predicates = new ArrayList<>();

        if (searchRequest.getProductStatus() != null) {
            predicates.add(cb.equal(root.get("productStatus"), searchRequest.getProductStatus()));
        }
        if (searchRequest.getProductAttributes() != null && !searchRequest.getProductAttributes().isEmpty()) {
            for (ProductAttribute productAttribute : searchRequest.getProductAttributes()) {
                predicates.add(cb.equal(
                        root.join("productAttribute").get("attribute").get("id"), productAttribute.getAtribut().getId()));
                predicates.add(cb.equal(
                        root.join("productAttribute").get("value"), productAttribute.getVrijednost()));
            }
        }
        if (searchRequest.getLocation() != null) {
            predicates.add(cb.equal(root.get("location"), searchRequest.getLocation()));

        }
        if (searchRequest.getCategoryName() != null) {
            predicates.add(cb.equal(root.get("categoryName"), searchRequest.getCategoryName()));
        }
        if (searchRequest.getPriceFrom() != null) {
            predicates.add(cb.equal(root.get("priceFrom"), searchRequest.getPriceFrom()));
        }
        if (searchRequest.getPriceTo() != null) {
            predicates.add(cb.equal(root.get("priceTo"), searchRequest.getPriceTo()));
        }

        query.where(predicates.toArray(new Predicate[0]));

        TypedQuery<ProizvodEntity> typedQuery = entityManager.createQuery(query);
        List<ProizvodEntity> productEntities = typedQuery.getResultList();
        List<Product> products = productEntities.stream().map(e -> modelMapper.map(e, Product.class)).toList();
        // Kreira Page koristeći listu proizvoda i ukupan broj proizvoda
        //return new PageImpl<>(products, page, products.size());
        return Util.getPage(page, products);
    }

    @Override
    public Product insert(ProductRequest productRequest, Authentication authentication) {
        JwtUser user = (JwtUser) authentication.getPrincipal();
        KorisnikEntity korisnikEntity = userRepository.findById(user.getId()).orElseThrow(NotFoundException::new);
        ProizvodEntity proizvodEntity = modelMapper.map(productRequest, ProizvodEntity.class);
        proizvodEntity.setId(null);
        proizvodEntity.setDatumKreiranja(new Date());
        proizvodEntity.setZavrsenaPonuda(0);
        proizvodEntity.setProdavac(korisnikEntity);
        proizvodEntity = productRepository.saveAndFlush(proizvodEntity);

        for (ImageRequest imageRequest : productRequest.getSlikas()) {
            SlikaEntity slikaEntity = modelMapper.map(imageRequest, SlikaEntity.class);
            slikaEntity.setProizvod(proizvodEntity);
            imageRepository.saveAndFlush(slikaEntity);
        }
        for (ProductAtributeRequest productAtributeRequest : productRequest.getProizvodAtributs()) {
            ProizvodAtributEntityPK proizvodAtributEntityPK = new ProizvodAtributEntityPK();
            proizvodAtributEntityPK.setProizvodId(proizvodEntity.getId());
            proizvodAtributEntityPK.setAtributId(productAtributeRequest.getAttributId());

            ProizvodAtributEntity proizvodAtributEntity = new ProizvodAtributEntity();
            proizvodAtributEntity.setId(proizvodAtributEntityPK);

            AtributEntity atribut = atributeRepository.findById(productAtributeRequest.getAttributId()).orElseThrow(NotFoundException::new);

            proizvodAtributEntity.setProizvod(proizvodEntity);
            proizvodAtributEntity.setAtribut(atribut);
            proizvodAtributEntity.setVrijednost(productAtributeRequest.getVrijednost());
            System.out.println("ciao");
            productAtributeRepository.saveAndFlush(proizvodAtributEntity);
        }
        // Product p = modelMapper.map(proizvodEntity, Product.class);
        logerService.insertLog("The new product has been added by user " + korisnikEntity.getKorisnickoIme(), this.getClass().getName());

        return modelMapper.map(proizvodEntity, Product.class);
    }

    @Override
    public void delete(Integer id) {
        ProizvodEntity proizvodEntity = productRepository.findById(id).orElseThrow(NotFoundException::new);
        proizvodEntity.setZavrsenaPonuda(2);
        logerService.insertLog("The user has deleted product" + proizvodEntity.getNaslov(), this.getClass().getName());

        productRepository.saveAndFlush(proizvodEntity);
    }


}
