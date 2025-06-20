package com.awbd.bookstore.repositories;

import com.awbd.bookstore.models.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // MODIFICAT: folosim userId în loc de User object
    @Query("SELECT o FROM Order o WHERE o.userId = :userId ORDER BY o.orderDate DESC")
    List<Order> findByUserIdOrderByOrderDateDesc(@Param("userId") Long userId); // istoric comenzi

    // Rămâne la fel - admin vede toate comenzile
    List<Order> findAllByOrderByOrderDateDesc(); // admin - istoric comenzi

    @Query("SELECT SUM(b.price) FROM Order o JOIN o.books b WHERE o.id = :orderId")
    Double calculateOrderTotal(@Param("orderId") Long orderId); // calcul total comandă

    // MODIFICAT: ștergem pe userId
    void deleteByUserId(Long userId);

    // MODIFICAT: găsim ultima comandă pe userId
    @Query("SELECT o FROM Order o WHERE o.userId = :userId ORDER BY o.orderDate DESC")
    Optional<Order> findFirstByUserIdOrderByOrderDateDesc(@Param("userId") Long userId);

    // ADĂUGAT: verificăm dacă user-ul are comenzi
    @Query("SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END FROM Order o WHERE o.userId = :userId")
    boolean existsByUserId(@Param("userId") Long userId);

    // ADĂUGAT: găsim toate comenzile pentru un user
    @Query("SELECT o FROM Order o WHERE o.userId = :userId")
    List<Order> findByUserId(@Param("userId") Long userId);
}