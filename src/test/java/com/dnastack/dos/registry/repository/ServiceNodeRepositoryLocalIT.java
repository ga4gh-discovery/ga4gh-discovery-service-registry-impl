package com.dnastack.dos.registry.repository;

import com.dnastack.dos.registry.DosRegistryApplication;
import com.dnastack.dos.registry.model.ServiceNode;
import com.dnastack.dos.registry.util.SecurityTestUtil;
import com.google.gson.Gson;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test cases against a local MySQL instance.
 * Assumption: Local MySQL instance is up and running.
 *
 * @Author: marchuang <br/>
 * @since: 1.0.0 <br/>
 */
@SpringBootTest(classes = DosRegistryApplication.class)
@RunWith(SpringRunner.class)
@ActiveProfiles({"it","local"})
@Ignore
public class ServiceNodeRepositoryLocalIT {

    public static final String OBJECTS_ENDPOINT = "/ga4gh/registry/v1/dataobjects";
    public static final String OAUTH_SIGNED_KEY = "Authorization";
    public static final String OAUTH_SIGNED_KEY_VALUE = "Dummy";

    @Autowired
    ServiceNodeRepository repository;

    Gson gson = new Gson();

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    FilterChainProxy springSecurityFilterChain;

    private MockMvc mockMvc;

    @Before
    public void cleanUp(){
        repository.deleteAll();
        prepareSomeDemoData();

        mockMvc =
                MockMvcBuilders.webAppContextSetup(wac)
                        .apply(springSecurity())
                        .build();
    }

    private void prepareSomeDemoData() {
        // given
        String ownerId = "demo-owner";
        String url = "http://localhost:3030";
        IntStream.range(0,100).forEach(i -> {
            String id = UUID.randomUUID().toString();
            String name = "test_dos_node-"+i;
            ServiceNode serviceNode = new ServiceNode();
            serviceNode.setOwnerId(ownerId);
            serviceNode.setId(id);
            serviceNode.setName(name);
            serviceNode.setUrl(url);
            Set<String> aliases = Stream.of("test-alias-1"+i, "test-alias-2"+i).collect(Collectors.toSet());
            serviceNode.setAliases(gson.toJson(aliases));
            Map<String, String> metadata = new HashMap<>();
            metadata.put("category", "cancer");
            metadata.put("kind", "kids");
            serviceNode.setMetaData(metadata);

            repository.save(serviceNode);

        });

    }

    private void prepareSomeDemoDataWithIdPrefix() {
        // given
        String nodeIdPrefix = "demo-node-";
        String ownerId = "demo-owner";
        String url = "http://localhost:3030";
        IntStream.range(0,100).forEach(i -> {
            String id = nodeIdPrefix+i;
            String name = "test_dos_node-"+i;
            ServiceNode serviceNode = new ServiceNode();
            serviceNode.setOwnerId(ownerId);
            serviceNode.setId(id);
            serviceNode.setName(name);
            serviceNode.setUrl(url);
            Set<String> aliases = Stream.of("test-alias-1"+i, "test-alias-2"+i).collect(Collectors.toSet());
            serviceNode.setAliases(gson.toJson(aliases));
            Map<String, String> metadata = new HashMap<>();
            metadata.put("category", "cancer");
            metadata.put("kind", "kids");
            serviceNode.setMetaData(metadata);

            repository.save(serviceNode);

        });

    }

    @Test
    public void getDataObjectsFromLocalNodeTest() throws Exception {

        MvcResult result = mockMvc.perform(
                get(OBJECTS_ENDPOINT)
                        .with(SecurityTestUtil.authDosUser())
                        .header(OAUTH_SIGNED_KEY, OAUTH_SIGNED_KEY_VALUE)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(jsonPath("$.dos_objects", isA(List.class)))
                .andExpect(jsonPath("$.dos_objects", hasSize(50))) //default page size
                .andExpect(jsonPath("$.next_page_token").exists())
                .andReturn();

    }

    @Test
    public void getDataObjectsFromLocalNodeTest_WithMoreParameters() throws Exception {

        repository.deleteAll();
        prepareSomeDemoDataWithIdPrefix();
        String nodeIds = "demo-node-0, demo-node-1, demo-node-2";
        MvcResult result = mockMvc.perform(
                get(OBJECTS_ENDPOINT)
                        .with(SecurityTestUtil.authDosUser())
                        .header(OAUTH_SIGNED_KEY, OAUTH_SIGNED_KEY_VALUE)
                        .param("dos_alias", "set0-test-5")
                        .param("node_ids", nodeIds)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(jsonPath("$.dos_objects", isA(List.class)))
                .andExpect(jsonPath("$.dos_objects", hasSize(3))) //default page size
                .andExpect(jsonPath("$.next_page_token").doesNotExist())
                .andReturn();

    }

    //"set1-name-3"

    @Test
    public void whenAddDataNode_thenReturnDataNode() {
        // given
        String id = "aaa-bbb-ccc";

        boolean exists = repository.exists(id);
        ServiceNode serviceNode;

        if (!exists) {
            //brand new entity
            String name = "test_dos_node";
            serviceNode = new ServiceNode();
            serviceNode.setId(id);
            serviceNode.setName(name);

            Set<String> aliases = Stream.of("test1", "test2").collect(Collectors.toSet());
            serviceNode.setAliases(gson.toJson(aliases));

            Map<String, String> metadata = new HashMap<>();
            metadata.put("category", "cancer");
            metadata.put("kind", "kids");
            serviceNode.setMetaData(metadata);
        } else {
            serviceNode = repository.findOne(id);
            //update what needs to be updated only...
            Map<String, String> metadata = serviceNode.getMetaData();
            metadata.put("additoinal", "additional");

            serviceNode.setMetaData(metadata);
        }
        repository.save(serviceNode);

        // when
        ServiceNode found = repository.findOne(serviceNode.getId());

        // then
        assertTrue(found.getName().equals(serviceNode.getName()));

        if(exists) {
            assertEquals(3, found.getMetaData().size());
            assertEquals(2, gson.toJson(found.getAliases()).length());
        } else {
            assertEquals(2, found.getMetaData().size());
            assertEquals(2, gson.toJson(found.getAliases()).length());

        }
    }

    @Test
    public void whenFetchDataNodeByPage_thenReturnDataNodePage() {
        // given
        String ownerId = "demo-owner";
        IntStream.range(0,100).forEach(i -> {
            String id = UUID.randomUUID().toString();
            String name = "test_dos_node-"+i;
            ServiceNode serviceNode = new ServiceNode();
            serviceNode.setOwnerId(ownerId);
            serviceNode.setId(id);
            serviceNode.setName(name);
            Set<String> aliases = Stream.of("test-alias-1", "test-alias-2").collect(Collectors.toSet());
            serviceNode.setAliases(gson.toJson(aliases));
            Map<String, String> metadata = new HashMap<>();
            metadata.put("category", "cancer");
            metadata.put("kind", "kids");
            serviceNode.setMetaData(metadata);

            repository.save(serviceNode);

        });

        // when
        Page<ServiceNode> serviceNodes = repository.findByOwnerId(ownerId, new PageRequest(0, 10));
        Page<ServiceNode> serviceNodesNextPage = repository.findByOwnerId(ownerId, new PageRequest(1, 10));


        // then
        assertEquals(10, serviceNodes.getSize());
    }

}