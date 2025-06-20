package com.awbd.bookstore.repositories;

import com.awbd.bookstore.models.Book;
import com.awbd.bookstore.models.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Category findByName(String name);
    boolean existsByName(String name);

    // carti dintr-o categorie
    @Query("SELECT c.books FROM Category c WHERE c.id = :categoryId")
    List<Book> findBooksByCategoryId(@Param("categoryId") Long categoryId);

    // categorii cu promotii
    @Query("SELECT c FROM Category c JOIN c.sales s WHERE s.isActive = true")
    List<Category> findCategoriesWithActiveSales();

}