package rs.ac.uns.ftn.isa.isa_project.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import rs.ac.uns.ftn.isa.isa_project.repository.ViewCountReplicaRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry koji drži informacije o svim replikama u sistemu.
 */
@Component
public class ReplicaService {

    private static final Logger LOG = LoggerFactory.getLogger(ReplicaService.class);

    @Autowired
    private ViewCountReplicaRepository replicaDAO;

    @Value("${crdt.replica.id}")
    private String currentReplicaId;

    @Value("${crdt.replica.urls:}")
    private String replicaUrlsConfig;

    private final Set<String> knownReplicaIds = ConcurrentHashMap.newKeySet();

    @PostConstruct
    public void initialize() {
        LOG.info("Initializing ReplicaRegistry...");

        registerReplica(currentReplicaId);

        List<String> otherReplicaIds = extractReplicaIdsFromUrls();
        for (String replicaId : otherReplicaIds) {
            registerReplica(replicaId);
        }

        LOG.info("ReplicaRegistry initialized with {} replicas: {}",
                knownReplicaIds.size(), knownReplicaIds);
    }

    public void registerReplica(String replicaId) {
        if (replicaId == null || replicaId.trim().isEmpty()) {
            return;
        }

        boolean isNew = knownReplicaIds.add(replicaId);
        if (isNew) {
            LOG.info("Registered new replica: {}", replicaId);
        }
    }

    public List<String> getAllReplicaIds() {
        return new ArrayList<>(knownReplicaIds);
    }

    public synchronized void ensureTableExists(String replicaId) {
        if (!replicaDAO.tableExists(replicaId)) {
            LOG.info("Table for replica {} does not exist. Creating...", replicaId);
            try {
                replicaDAO.createTableIfNotExists(replicaId);
                LOG.info("Successfully created table for replica {}", replicaId);
            } catch (Exception e) {
                LOG.error("Failed to create table for replica {}: {}", replicaId, e.getMessage());
                throw new RuntimeException("Failed to create table for replica " + replicaId, e);
            }
        }

        registerReplica(replicaId);
    }

    private List<String> extractReplicaIdsFromUrls() {
        if (replicaUrlsConfig == null || replicaUrlsConfig.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<String> replicaIds = new ArrayList<>();
        String[] urls = replicaUrlsConfig.split(",");

        for (String url : urls) {
            url = url.trim();
            if (url.isEmpty()) {
                continue;
            }

            try {
                String hostname = url.replaceFirst("https?://", "")
                        .split(":")[0]
                        .split("/")[0];

                String replicaId = hostnameToReplicaId(hostname);
                replicaIds.add(replicaId);

            } catch (Exception e) {
                LOG.warn("Failed to parse replica ID from URL: {}", url);
            }
        }

        return replicaIds;
    }

    private String hostnameToReplicaId(String hostname) {
        String number = hostname.replaceAll("\\D+", "");

        if (number.isEmpty()) {
            return "replica-" + hostname;
        }

        return "replica-" + number;
    }

    public String getCurrentReplicaId() {
        return currentReplicaId;
    }

    public int getReplicaCount() {
        return knownReplicaIds.size();
    }
}