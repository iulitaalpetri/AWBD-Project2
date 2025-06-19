package com.awbd.bookstore.repositories;


import com.awbd.bookstore.models.Order;
import com.awbd.bookstore.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserOrderByOrderDateDesc(User user); // istoric comenzi

    List<Order> findAllByOrderByOrderDateDesc(); // admin - istoric comenzi

    @Query("SELECT SUM(b.price) FROM Order o JOIN o.books b WHERE o.id = :orderId")
    Double calculateOrderTotal(@Param("orderId") Long orderId); // calcul total comandă

    void deleteByUser(User user);

    Optional<Order> findFirstByUserOrderByOrderDateDesc(User user);






}