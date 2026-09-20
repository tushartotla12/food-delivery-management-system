package org.dmg.Repositories;

import org.dmg.Entities.Payment;
import org.springframework.data.jpa.repository.JpaRepository;


public interface PaymentRepository extends JpaRepository<Payment, Long> {

}

