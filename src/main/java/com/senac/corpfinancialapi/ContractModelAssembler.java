package com.senac.corpfinancialapi;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
class ContractModelAssembler implements RepresentationModelAssembler<Contract, EntityModel<Contract>> {

    @Override
    public EntityModel<Contract> toModel(Contract contract) {
        return EntityModel.of(contract,
                linkTo(methodOn(ContractController.class).getContractById(contract.getId())).withSelfRel(),
                linkTo(methodOn(ContractController.class).getAllContracts(Pageable.unpaged())).withRel("contracts"));
    }
}
