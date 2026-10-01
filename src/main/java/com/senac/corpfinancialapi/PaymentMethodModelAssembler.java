package com.senac.corpfinancialapi;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
class PaymentMethodModelAssembler implements RepresentationModelAssembler<PaymentMethod, EntityModel<PaymentMethod>> {

    @Override
    public EntityModel<PaymentMethod> toModel(PaymentMethod paymentMethod) {
        return EntityModel.of(paymentMethod,
                linkTo(methodOn(PaymentMethodController.class).getPaymentMethodById(paymentMethod.getId())).withSelfRel(),
                linkTo(methodOn(PaymentMethodController.class).getAllPaymentMethods(Pageable.unpaged())).withRel("payment-methods"));
    }
}
