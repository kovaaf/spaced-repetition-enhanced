package org.company.config.properties;

import lombok.extern.slf4j.Slf4j;
import org.company.domain.ServerInfo;

import java.util.List;
import java.util.stream.Collectors;

/**
 * DataConfig mirrors the {@code data} section of the YAML file.
 */
@Slf4j
public record DataProperties(List<ServerProperties> servers) {

    /**
     * Returns a list of {@link ServerInfo} domain objects built from the configuration.
     *
     * @return list of server information
     */
    public List<ServerInfo> getServersInfo() {
        if (servers == null) {
            return List.of();
        }
        return servers.stream().map(s -> new ServerInfo(s.name(), s.url())).collect(Collectors.toList());
    }
}
