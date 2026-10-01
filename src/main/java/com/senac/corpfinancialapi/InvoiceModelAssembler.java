package com.senac.corpfinancialapi;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
class InvoiceModelAssembler implements RepresentationModelAssembler<Invoice, EntityModel<Invoice>> {

    @Override
    public EntityModel<Invoice> toModel(Invoice invoice) {
        return EntityModel.of(invoice,
                linkTo(methodOn(InvoiceController.class).getInvoiceById(invoice.getId())).withSelfRel(),
                linkTo(methodOn(InvoiceController.class).getAllInvoices(Pageable.unpaged())).withRel("invoices"));
    }
}
