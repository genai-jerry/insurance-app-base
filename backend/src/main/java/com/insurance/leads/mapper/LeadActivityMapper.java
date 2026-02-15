package com.insurance.leads.mapper;

import com.insurance.common.entity.LeadActivity;
import com.insurance.leads.dto.CreateLeadActivityRequest;
import com.insurance.leads.dto.LeadActivityDto;
import org.mapstruct.*;

/**
 * MapStruct mapper for LeadActivity entity and DTOs
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface LeadActivityMapper {

    @Mapping(source = "lead.id", target = "leadId")
    LeadActivityDto toDto(LeadActivity activity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "lead", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    LeadActivity toEntity(CreateLeadActivityRequest request);
}
