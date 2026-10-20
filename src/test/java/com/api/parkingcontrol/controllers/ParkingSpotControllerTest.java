package com.api.parkingcontrol.controllers;

import com.api.parkingcontrol.configs.DateConfig;
import com.api.parkingcontrol.models.ParkingSpotModel;
import com.api.parkingcontrol.services.ParkingSpotService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes da camada web (sem banco de dados): o ParkingSpotService é simulado com Mockito.
 */
@WebMvcTest(ParkingSpotController.class)
@Import(DateConfig.class)
class ParkingSpotControllerTest {

    private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final LocalDateTime REGISTRATION_DATE = LocalDateTime.of(2026, 10, 19, 15, 0, 0);

    private static final String VALID_JSON = "{"
            + "\"parkingSpotNumber\":\"205B\","
            + "\"licensePlateCar\":\"RRS8562\","
            + "\"brandCar\":\"Audi\","
            + "\"modelCar\":\"Q5\","
            + "\"colorCar\":\"Preto\","
            + "\"responsibleName\":\"Maria Souza\","
            + "\"apartment\":\"205\","
            + "\"block\":\"B\"}";

    @Autowired
    MockMvc mockMvc;

    @MockBean
    ParkingSpotService parkingSpotService;

    private ParkingSpotModel existing() {
        var model = new ParkingSpotModel();
        model.setId(ID);
        model.setRegistrationDate(REGISTRATION_DATE);
        model.setParkingSpotNumber("205B");
        model.setLicensePlateCar("RRS8562");
        model.setBrandCar("Audi");
        model.setModelCar("Q5");
        model.setColorCar("Preto");
        model.setResponsibleName("Maria Souza");
        model.setApartment("205");
        model.setBlock("B");
        return model;
    }

    // 1. Listagem sem registros
    @Test
    void listEmpty() throws Exception {
        when(parkingSpotService.findAll()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/parking-spot"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    // 2. Cadastro válido (id e registrationDate enviados pelo cliente são ignorados)
    @Test
    void createValid() throws Exception {
        when(parkingSpotService.save(any())).thenAnswer(inv -> {
            var saved = new ParkingSpotModel();
            BeanUtils.copyProperties(inv.getArgument(0), saved);
            saved.setId(ID);
            return saved;
        });
        String body = VALID_JSON.replace("{", "{\"id\":\"22222222-2222-2222-2222-222222222222\","
                + "\"registrationDate\":\"2000-01-01T00:00:00Z\",");

        mockMvc.perform(post("/parking-spot").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ID.toString()))
                .andExpect(jsonPath("$.licensePlateCar").value("RRS8562"))
                .andExpect(jsonPath("$.registrationDate").value(matchesPattern(
                        "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z")));

        ArgumentCaptor<ParkingSpotModel> captor = ArgumentCaptor.forClass(ParkingSpotModel.class);
        verify(parkingSpotService).save(captor.capture());
        assertNull(captor.getValue().getId());
        assertNotEquals(2000, captor.getValue().getRegistrationDate().getYear());
    }

    // 3. Cadastro com campos obrigatórios vazios
    @Test
    void createWithBlankFields() throws Exception {
        String body = VALID_JSON.replace("\"RRS8562\"", "\"\"").replace("\"Audi\"", "\"   \"");

        mockMvc.perform(post("/parking-spot").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Dados inválidos. Verifique os campos informados."))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.errors.licensePlateCar").value("A placa do veículo é obrigatória."))
                .andExpect(jsonPath("$.errors.brandCar").value("A marca do veículo é obrigatória."));
        verify(parkingSpotService, never()).save(any());
    }

    @Test
    void createWithMissingFieldsAndTooLongValues() throws Exception {
        String body = "{\"parkingSpotNumber\":\"12345678901\",\"licensePlateCar\":\"ABC-1234\"}";

        mockMvc.perform(post("/parking-spot").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.parkingSpotNumber").value("O número da vaga deve ter no máximo 10 caracteres."))
                .andExpect(jsonPath("$.errors.licensePlateCar").value("A placa do veículo deve ter no máximo 7 caracteres."))
                .andExpect(jsonPath("$.errors.block").value("O bloco é obrigatório."));
    }

    // 4. Cadastro com placa duplicada
    @Test
    void createDuplicatePlate() throws Exception {
        when(parkingSpotService.existsByLicensePlateCar("RRS8562")).thenReturn(true);

        mockMvc.perform(post("/parking-spot").contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Esta placa já está cadastrada."))
                .andExpect(jsonPath("$", not(hasKey("errors"))));
        verify(parkingSpotService, never()).save(any());
    }

    // 5. Cadastro com vaga duplicada
    @Test
    void createDuplicateParkingSpotNumber() throws Exception {
        when(parkingSpotService.existsByParkingSpotNumber("205B")).thenReturn(true);

        mockMvc.perform(post("/parking-spot").contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Este número de vaga já está em uso."));
    }

    @Test
    void createDuplicateApartmentAndBlock() throws Exception {
        when(parkingSpotService.existsByApartmentAndBlock("205", "B")).thenReturn(true);

        mockMvc.perform(post("/parking-spot").contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Já existe uma vaga cadastrada para este apartamento/bloco."));
    }

    // Conflito detectado pelo banco (ex.: requisições simultâneas)
    @Test
    void createConcurrentConflictFromDatabase() throws Exception {
        when(parkingSpotService.save(any())).thenThrow(new DataIntegrityViolationException("duplicate key uk_xyz"));

        mockMvc.perform(post("/parking-spot").contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(
                        "Não foi possível salvar: já existe uma vaga com esta placa ou este número de vaga."));
    }

    // 6. Busca por UUID existente
    @Test
    void getExisting() throws Exception {
        when(parkingSpotService.findById(ID)).thenReturn(Optional.of(existing()));

        mockMvc.perform(get("/parking-spot/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ID.toString()))
                .andExpect(jsonPath("$.parkingSpotNumber").value("205B"));
    }

    // 7. Busca por UUID inexistente
    @Test
    void getMissing() throws Exception {
        when(parkingSpotService.findById(ID)).thenReturn(Optional.empty());

        mockMvc.perform(get("/parking-spot/" + ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Vaga de estacionamento não encontrada."));
    }

    @Test
    void getWithInvalidUuid() throws Exception {
        mockMvc.perform(get("/parking-spot/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Valor inválido para o parâmetro 'id'."));
    }

    // 8. Atualização válida (id e registrationDate preservados)
    @Test
    void updateValid() throws Exception {
        when(parkingSpotService.findById(ID)).thenReturn(Optional.of(existing()));
        when(parkingSpotService.save(any())).thenAnswer(inv -> inv.getArgument(0));
        String body = VALID_JSON.replace("\"Preto\"", "\"Branco\"")
                .replace("{", "{\"id\":\"22222222-2222-2222-2222-222222222222\",\"registrationDate\":\"2000-01-01T00:00:00Z\",");

        mockMvc.perform(put("/parking-spot/" + ID).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ID.toString()))
                .andExpect(jsonPath("$.colorCar").value("Branco"))
                .andExpect(jsonPath("$.registrationDate").value("2026-10-19T15:00:00Z"));
    }

    // 9. Atualização mantendo os mesmos dados (o próprio registro é desconsiderado nas duplicidades)
    @Test
    void updateKeepingSameData() throws Exception {
        when(parkingSpotService.findById(ID)).thenReturn(Optional.of(existing()));
        when(parkingSpotService.save(any())).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(put("/parking-spot/" + ID).contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isOk());

        verify(parkingSpotService).existsByLicensePlateCarAndIdNot("RRS8562", ID);
        verify(parkingSpotService).existsByParkingSpotNumberAndIdNot("205B", ID);
        verify(parkingSpotService).existsByApartmentAndBlockAndIdNot("205", "B", ID);
        verify(parkingSpotService, never()).existsByLicensePlateCar(anyString());
    }

    // 10. Atualização tentando utilizar placa de outro registro
    @Test
    void updateWithPlateOfAnotherRecord() throws Exception {
        when(parkingSpotService.findById(ID)).thenReturn(Optional.of(existing()));
        when(parkingSpotService.existsByLicensePlateCarAndIdNot("RRS8562", ID)).thenReturn(true);

        mockMvc.perform(put("/parking-spot/" + ID).contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Esta placa já está cadastrada."));
        verify(parkingSpotService, never()).save(any());
    }

    // 11. Atualização tentando utilizar vaga de outro registro
    @Test
    void updateWithParkingSpotNumberOfAnotherRecord() throws Exception {
        when(parkingSpotService.findById(ID)).thenReturn(Optional.of(existing()));
        when(parkingSpotService.existsByParkingSpotNumberAndIdNot("205B", ID)).thenReturn(true);

        mockMvc.perform(put("/parking-spot/" + ID).contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Este número de vaga já está em uso."));
        verify(parkingSpotService, never()).save(any());
    }

    @Test
    void updateMissing() throws Exception {
        when(parkingSpotService.findById(ID)).thenReturn(Optional.empty());

        mockMvc.perform(put("/parking-spot/" + ID).contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Vaga de estacionamento não encontrada."));
    }

    @Test
    void updateWithInvalidData() throws Exception {
        mockMvc.perform(put("/parking-spot/" + ID).contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_JSON.replace("\"205\"", "\"\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.apartment").value("O apartamento é obrigatório."));
    }

    // Endpoints POST alternativos de edição aplicam as mesmas regras do PUT
    @Test
    void alternativeEditEndpoints() throws Exception {
        when(parkingSpotService.findById(ID)).thenReturn(Optional.of(existing()));
        when(parkingSpotService.save(any())).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/parking-spot/edit-param/" + ID).contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ID.toString()));

        mockMvc.perform(post("/parking-spot/edit").contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_JSON.replace("{", "{\"id\":\"" + ID + "\",")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ID.toString()));

        mockMvc.perform(post("/parking-spot/edit").contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("O campo id é obrigatório para editar uma vaga."));

        when(parkingSpotService.existsByLicensePlateCarAndIdNot("RRS8562", ID)).thenReturn(true);
        mockMvc.perform(post("/parking-spot/edit-param/" + ID).contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isConflict());
    }

    // 12. Exclusão válida
    @Test
    void deleteValid() throws Exception {
        when(parkingSpotService.findById(ID)).thenReturn(Optional.of(existing()));

        mockMvc.perform(delete("/parking-spot/" + ID))
                .andExpect(status().isOk())
                .andExpect(content().string("Parking Spot deleted successfully."));
        verify(parkingSpotService).delete(any());

        mockMvc.perform(post("/parking-spot/delete/" + ID))
                .andExpect(status().isOk())
                .andExpect(content().string("Parking Spot deleted successfully."));
    }

    // 13. Exclusão de registro inexistente
    @Test
    void deleteMissing() throws Exception {
        when(parkingSpotService.findById(ID)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/parking-spot/" + ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
        verify(parkingSpotService, never()).delete(any());
    }

    // 14. Listagem paginada
    @Test
    void paginatedList() throws Exception {
        when(parkingSpotService.findAllOrder(any())).thenAnswer(inv -> {
            Pageable pageable = inv.getArgument(0);
            return new PageImpl<>(List.of(existing()), pageable, 11);
        });

        mockMvc.perform(get("/parking-spot/order").param("page", "1").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(ID.toString()))
                .andExpect(jsonPath("$.totalElements").value(11))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.size").value(5));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(parkingSpotService).findAllOrder(captor.capture());
        assertEquals(PageRequest.of(1, 5, Sort.by("id")), captor.getValue());
    }

    // 15. Serialização de datas
    @Test
    void dateSerialization() throws Exception {
        when(parkingSpotService.findAll()).thenReturn(List.of(existing()));

        mockMvc.perform(get("/parking-spot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].registrationDate").value("2026-10-19T15:00:00Z"));
    }

    // Erros de formato da requisição
    @Test
    void malformedJsonAndWrongContentType() throws Exception {
        mockMvc.perform(post("/parking-spot").contentType(MediaType.APPLICATION_JSON).content("{invalido"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Corpo da requisição ausente ou com JSON inválido."));

        mockMvc.perform(post("/parking-spot").contentType(MediaType.TEXT_PLAIN).content(VALID_JSON))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415));

        mockMvc.perform(put("/parking-spot"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    void unknownFieldsAreIgnored() throws Exception {
        when(parkingSpotService.save(any())).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/parking-spot").contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_JSON.replace("{", "{\"campoExtra\":\"x\",")))
                .andExpect(status().isCreated());
    }

    @Test
    void unexpectedErrorDoesNotExposeDetails() throws Exception {
        when(parkingSpotService.findAll()).thenThrow(new IllegalStateException("detalhe interno"));

        mockMvc.perform(get("/parking-spot"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Erro interno no servidor."))
                .andExpect(content().string(not(containsString("detalhe interno"))));
    }

    @Test
    void invalidSortField() throws Exception {
        when(parkingSpotService.findAllOrder(eq(PageRequest.of(0, 10, Sort.by("xyz")))))
                .thenThrow(new PropertyReferenceException(
                        "xyz", ClassTypeInformation.from(ParkingSpotModel.class), Collections.emptyList()));

        mockMvc.perform(get("/parking-spot/order").param("sort", "xyz"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Campo de ordenação inválido: 'xyz'."));
    }

}
