/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.is.key.manager.operations.endpoint.userinfo.util;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.wso2.carbon.claim.mgt.ClaimManagerHandler;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.application.common.model.Claim;
import org.wso2.carbon.identity.application.common.model.ClaimMapping;
import org.wso2.carbon.identity.claim.metadata.mgt.ClaimMetadataHandler;
import org.wso2.is.key.manager.operations.endpoint.dto.ClaimDTO;
import org.wso2.is.key.manager.operations.endpoint.dto.ClaimListDTO;
import org.wso2.is.key.manager.operations.endpoint.dto.ErrorDTO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility class for Claims.
 */
public class UserInfoUtil {

    private static final String OIDC_DIALECT_URI = "http://wso2.org/oidc/claim";

    public static ErrorDTO getError(String code, String message, String description) {

        ErrorDTO errorDTO = new ErrorDTO();
        errorDTO.setCode(code);
        errorDTO.setMessage(message);
        errorDTO.setDescription(description);
        return errorDTO;
    }

    public static ClaimListDTO getListDTOfromClaimsMap(Map<String, String> claims) {

        ClaimListDTO listDto = new ClaimListDTO();
        List<ClaimDTO> list = new ArrayList<ClaimDTO>();
        listDto.setCount(claims.size());
        for (Map.Entry<String, String> claimEntry : claims.entrySet()) {
            ClaimDTO dto = new ClaimDTO();
            dto.setUri(claimEntry.getKey());
            dto.setValue(claimEntry.getValue());
            list.add(dto);
        }
        listDto.setList(list);
        return listDto;
    }

    @SuppressFBWarnings("DLS_DEAD_LOCAL_STORE")
    public static Map<String, String> convertClaimMap(Map<ClaimMapping, String> userAttributes, String username,
                                                      String dialect, boolean convertDialect) throws Exception {

        Map<String, String> userClaims = new HashMap<>();
        Map<String, String> userClaimsCopy = new HashMap<>();
        for (Map.Entry<ClaimMapping, String> entry : userAttributes.entrySet()) {
            Claim claimObject = entry.getKey().getLocalClaim();
            if (claimObject == null) {
                claimObject = entry.getKey().getRemoteClaim();
            }
            userClaims.put(claimObject.getClaimUri(), entry.getValue());
            userClaimsCopy.put(claimObject.getClaimUri(), entry.getValue());
        }

        if (!convertDialect) {
            return userClaims;
        }

        String tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();

        Map<String, String> configuredDialectToCarbonClaimMapping = null; // (key) configuredDialectClaimURI -> (value)
        // carbonClaimURI
        Map<String, String> carbonToOIDCclaimMapping = null; // (key) carbonClaimURI -> value (oidcClaimURI)

        Set<String> claimUris = new HashSet<String>(userClaims.keySet());

        carbonToOIDCclaimMapping = new ClaimMetadataHandler().getMappingsMapFromOtherDialectToCarbon(OIDC_DIALECT_URI,
                claimUris, tenantDomain, true);
        configuredDialectToCarbonClaimMapping = ClaimManagerHandler.getInstance()
                .getMappingsMapFromCarbonDialectToOther(dialect, carbonToOIDCclaimMapping.keySet(), tenantDomain);

        for (Map.Entry<String, String> oidcClaimValEntry : userClaims.entrySet()) {
            for (Map.Entry<String, String> carbonToOIDCEntry : carbonToOIDCclaimMapping.entrySet()) {
                if (oidcClaimValEntry.getKey().equals(carbonToOIDCEntry.getValue())) {
                    for (Map.Entry<String, String> configuredToCarbonEntry : configuredDialectToCarbonClaimMapping
                            .entrySet()) {
                        if (configuredToCarbonEntry.getValue().equals(carbonToOIDCEntry.getKey())) {
                            userClaimsCopy.remove(oidcClaimValEntry.getKey());
                            userClaimsCopy.put(configuredToCarbonEntry.getKey(), oidcClaimValEntry.getValue());
                        }
                    }
                }
            }
        }

        return userClaimsCopy;
    }

}
