package com.insurance.leads.mapper;

import com.insurance.common.entity.Lead;
import com.insurance.leads.dto.CreateLeadRequest;
import com.insurance.leads.dto.LeadDto;
import org.mapstruct.*;

/**
 * MapStruct mapper for Lead entity and DTOs
 * This is an alternative to manual conversion in the service layer
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface LeadMapper {

    @Mapping(source = "assignedAgent.id", target = "assignedAgentId")
    @Mapping(source = "assignedAgent.name", target = "assignedAgentName")
    LeadDto toDto(Lead lead);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "assignedAgent", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Lead toEntity(CreateLeadRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "assignedAgent", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(CreateLeadRequest request, @MappingTarget Lead lead);
}
