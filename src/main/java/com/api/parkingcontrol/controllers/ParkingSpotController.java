package com.api.parkingcontrol.controllers;

import com.api.parkingcontrol.dtos.ErrorResponseDto;
import com.api.parkingcontrol.dtos.ParkingSpotDto;
import com.api.parkingcontrol.models.ParkingSpotModel;
import com.api.parkingcontrol.services.ParkingSpotService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// CORS configurado globalmente em configs/CorsConfig (origens definidas em application-old.properties)
@RestController
@RequestMapping("parking-spot")
public class ParkingSpotController {

    @Autowired
    ParkingSpotService parkingSpotService;


    @PostMapping
    public ResponseEntity<Object> saveParkingSpot(@RequestBody @Valid ParkingSpotDto parkingSpotDto){
        ResponseEntity<Object> conflict = checkConflicts(parkingSpotDto, null);
        if (conflict != null) {
            return conflict;
        }
        var parkingSpotModel = new ParkingSpotModel();
        // id e registrationDate são definidos pelo backend, nunca pelo cliente
        BeanUtils.copyProperties(parkingSpotDto, parkingSpotModel, "id", "registrationDate");
        parkingSpotModel.setRegistrationDate(LocalDateTime.now(ZoneId.of("UTC")));
        return ResponseEntity.status(HttpStatus.CREATED).body(parkingSpotService.save(parkingSpotModel));
    }

    @GetMapping
    public ResponseEntity<List<ParkingSpotModel>> getAllParkingSpots(){
        return ResponseEntity.status(HttpStatus.OK).body(parkingSpotService.findAll());
    }

    @GetMapping("/order")
    public ResponseEntity<Page<ParkingSpotModel>> getAllParkingSpotsOrder(@PageableDefault(page = 0, size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable){
        return ResponseEntity.status(HttpStatus.OK).body(parkingSpotService.findAllOrder(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> getOneParkingSpot(@PathVariable(value = "id") UUID id){
        Optional<ParkingSpotModel> parkingSpotModelOptional = parkingSpotService.findById(id);
        if (!parkingSpotModelOptional.isPresent()) {
            return notFound();
        }
        return ResponseEntity.status(HttpStatus.OK).body(parkingSpotModelOptional.get());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deleteParkingSpot(@PathVariable(value = "id") UUID id){
        Optional<ParkingSpotModel> parkingSpotModelOptional = parkingSpotService.findById(id);
        if (!parkingSpotModelOptional.isPresent()) {
            return notFound();
        }
        parkingSpotService.delete(parkingSpotModelOptional.get());
        return ResponseEntity.status(HttpStatus.OK).body("Parking Spot deleted successfully.");
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> updateParkingSpot(@PathVariable(value = "id") UUID id,
                                                    @RequestBody @Valid ParkingSpotDto parkingSpotDto){
        return update(id, parkingSpotDto);
    }

    // Alternativa ao PUT /{id} (mesmo comportamento)
    @PostMapping("/edit-param/{id}")
    public ResponseEntity<Object> editParamParkingSpot(@PathVariable(value = "id") UUID id,
                                                       @RequestBody @Valid ParkingSpotDto parkingSpotDto){
        return update(id, parkingSpotDto);
    }

    // Alternativa ao PUT /{id}: o id vai no corpo da requisição
    @PostMapping("/edit")
    public ResponseEntity<Object> editParkingSpot(@RequestBody @Valid ParkingSpotDto parkingSpotDto){
        if (parkingSpotDto.getId() == null) {
            return error(HttpStatus.BAD_REQUEST, "O campo id é obrigatório para editar uma vaga.");
        }
        return update(parkingSpotDto.getId(), parkingSpotDto);
    }

    // Alternativa ao DELETE /{id} (mesmo comportamento)
    @PostMapping("/delete/{id}")
    public ResponseEntity<Object> deletePostParkingSpot(@PathVariable(value = "id") UUID id){
        Optional<ParkingSpotModel> parkingSpotModelOptional = parkingSpotService.findById(id);
        if (!parkingSpotModelOptional.isPresent()) {
            return notFound();
        }
        parkingSpotService.delete(parkingSpotModelOptional.get());
        return ResponseEntity.status(HttpStatus.OK).body("Parking Spot deleted successfully.");
    }

    private ResponseEntity<Object> update(UUID id, ParkingSpotDto parkingSpotDto) {
        Optional<ParkingSpotModel> parkingSpotModelOptional = parkingSpotService.findById(id);
        if (!parkingSpotModelOptional.isPresent()) {
            return notFound();
        }
        ResponseEntity<Object> conflict = checkConflicts(parkingSpotDto, id);
        if (conflict != null) {
            return conflict;
        }
        var parkingSpotModel = new ParkingSpotModel();
        BeanUtils.copyProperties(parkingSpotDto, parkingSpotModel, "id", "registrationDate");
        parkingSpotModel.setId(parkingSpotModelOptional.get().getId());
        parkingSpotModel.setRegistrationDate(parkingSpotModelOptional.get().getRegistrationDate());
        return ResponseEntity.status(HttpStatus.OK).body(parkingSpotService.save(parkingSpotModel));
    }

    /**
     * Verifica placa, número da vaga e apartamento/bloco já utilizados.
     * Na edição (id != null) o próprio registro é desconsiderado.
     * Retorna null quando não há conflito.
     */
    private ResponseEntity<Object> checkConflicts(ParkingSpotDto parkingSpotDto, UUID id) {
        boolean licensePlateInUse = id == null
                ? parkingSpotService.existsByLicensePlateCar(parkingSpotDto.getLicensePlateCar())
                : parkingSpotService.existsByLicensePlateCarAndIdNot(parkingSpotDto.getLicensePlateCar(), id);
        if (licensePlateInUse) {
            return error(HttpStatus.CONFLICT, "Esta placa já está cadastrada.");
        }
        boolean parkingSpotNumberInUse = id == null
                ? parkingSpotService.existsByParkingSpotNumber(parkingSpotDto.getParkingSpotNumber())
                : parkingSpotService.existsByParkingSpotNumberAndIdNot(parkingSpotDto.getParkingSpotNumber(), id);
        if (parkingSpotNumberInUse) {
            return error(HttpStatus.CONFLICT, "Este número de vaga já está em uso.");
        }
        boolean apartmentAndBlockInUse = id == null
                ? parkingSpotService.existsByApartmentAndBlock(parkingSpotDto.getApartment(), parkingSpotDto.getBlock())
                : parkingSpotService.existsByApartmentAndBlockAndIdNot(parkingSpotDto.getApartment(), parkingSpotDto.getBlock(), id);
        if (apartmentAndBlockInUse) {
            return error(HttpStatus.CONFLICT, "Já existe uma vaga cadastrada para este apartamento/bloco.");
        }
        return null;
    }

    private ResponseEntity<Object> notFound() {
        return error(HttpStatus.NOT_FOUND, "Vaga de estacionamento não encontrada.");
    }

    private ResponseEntity<Object> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ErrorResponseDto(status.value(), message));
    }

}
