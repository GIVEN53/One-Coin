package OneCoin.Server.order.controller;

import OneCoin.Server.order.mapper.TransactionHistoryMapper;
import OneCoin.Server.order.service.TransactionHistoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TransactionHistoryController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
@MockBean(JpaMetamodelMappingContext.class)
public class TransactionHistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionHistoryService transactionHistoryService;

    @MockBean
    private TransactionHistoryMapper mapper;

    @Test
    @DisplayName("slice test: 코인 하나의 내역만 조회")
    void getHistoryByCode() throws Exception {
        // when then
        mockMvc.perform(
                        get("/api/order/completion/{code}", "KRW-BTC")
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk());
    }
}
