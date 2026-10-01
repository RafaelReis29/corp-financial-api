package com.senac.corpfinancialapi;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
class CompanyModelAssembler implements RepresentationModelAssembler<Company, EntityModel<Company>> {

    @Override
    public EntityModel<Company> toModel(Company company) {
        return EntityModel.of(company,
                linkTo(methodOn(CompanyController.class).getCompanyById(company.getId())).withSelfRel(),
                linkTo(methodOn(CompanyController.class).getAllCompanies(Pageable.unpaged())).withRel("companies"));
    }
}
