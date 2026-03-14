package com.obar.bll;

import com.obar.dal.PaymentMethodRepository;
import com.obar.dal.PaymentRepository;
import com.obar.model.Payment;
import com.obar.model.PaymentMethod;
import com.obar.model.enums.PaymentStatus;

import java.util.List;
import java.util.Optional;

public class PaymentService {

    private final PaymentRepository paymentRepository = new PaymentRepository();
    private final PaymentMethodRepository paymentMethodRepository = new PaymentMethodRepository();

    public Payment processPayment(Payment payment) {
        payment.setStatus(PaymentStatus.PENDING);
        Payment saved = paymentRepository.save(payment);

        // Aqui futuramente integraria com gateway de pagamento
        saved.setStatus(PaymentStatus.PROCESSED);
        return paymentRepository.update(saved);
    }

    public Optional<Payment> findById(Integer id) {
        return paymentRepository.findById(id);
    }

    public List<Payment> findByTrip(Integer tripId) {
        return paymentRepository.findByTripId(tripId);
    }

    public PaymentMethod addPaymentMethod(PaymentMethod method) {
        return paymentMethodRepository.save(method);
    }

    public List<PaymentMethod> findMethodsByClient(Integer clientId) {
        return paymentMethodRepository.findByClientId(clientId);
    }

    public void deactivatePaymentMethod(Integer methodId) {
        paymentMethodRepository.findById(methodId).ifPresent(m -> {
            m.setActive(false);
            paymentMethodRepository.update(m);
        });
    }
}