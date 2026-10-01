package com.senac.corpfinancialapi;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
class ContactModelAssembler implements RepresentationModelAssembler<Contact, EntityModel<Contact>> {

    @Override
    public EntityModel<Contact> toModel(Contact contact) {
        return EntityModel.of(contact,
                linkTo(methodOn(ContactController.class).getContactById(contact.getId())).withSelfRel(),
                linkTo(methodOn(ContactController.class).getAllContacts(Pageable.unpaged())).withRel("contacts"));
    }
}
