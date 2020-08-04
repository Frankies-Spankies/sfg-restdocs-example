package guru.springframework.sfgrestdocsexample.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import guru.springframework.sfgrestdocsexample.domain.Beer;
import guru.springframework.sfgrestdocsexample.repositories.BeerRepository;
import guru.springframework.sfgrestdocsexample.web.model.BeerDto;
import guru.springframework.sfgrestdocsexample.web.model.BeerStyleEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.constraints.ConstraintDescriptions;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.PayloadDocumentation;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.StringUtils;


import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

//****** Hay que cambiar el import del Builders del MockMvc al de rest docs para que funcione con el "Documentador"********
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.restdocs.snippet.Attributes.key;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


//Configura MockMVC para que funcione con RestDocumentation
//Esta forma de configurar permite que no se levante todo el contexto de spring, dejndo lo que provee @WebMvcTest
//Mas referencia https://docs.spring.io/spring-restdocs/docs/2.0.4.RELEASE/reference/html5/
@ExtendWith(RestDocumentationExtension.class)
@AutoConfigureRestDocs(uriScheme = "http", uriHost = "dev.frank.cursos", uriPort = 80)
//Solo carga el controlador con el runner de Mockito por eso ocupa el @ComponentScan
//Mas referencia https://stackoverflow.com/questions/39865596/difference-between-using-mockmvc-with-springboottest-and-using-webmvctest
@WebMvcTest(BeerController.class)
@ComponentScan(basePackages = "guru.springframework.sfgrestdocsexample.web.mappers")
class BeerControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    BeerRepository beerRepository;

    @Test
    void getBeerById() throws Exception {
        given(beerRepository.findById(any())).willReturn(Optional.of(Beer.builder().build()));

        //Al ser un solo parametro se pasa en el path, los parametros del path se tienen que poner explicitos
        ResultActions perform = mockMvc.perform(get("/api/v1/beer/{beerId}", UUID.randomUUID().toString()) //Path parameters
                .param("is cold", "yes")//query parameters en post son campos de formulario
                .accept(MediaType.APPLICATION_JSON));

                perform.andExpect(status().isOk());

                //El nombre del documento No debe empezar con diagonal p.e /v1/beer
                //Al agregar -get la documentacion de este endpoint se separa de el endpoint v1/beer-new
                perform.andDo(document("v1/beer-get",
                        pathParameters(
                                parameterWithName("beerId").description("UUID de la cerveza deseada")),
                        requestParameters(
                                parameterWithName("is cold").description("Is Beer Cold Query parameter")),
                        responseFields(
                                fieldWithPath("id").description("Id of de Beer"),
                                fieldWithPath("version").description("Version of the beer"),
                                fieldWithPath("createdDate").description("Creation date"),
                                fieldWithPath("lastModifiedDate").description("Last modified date"),
                                fieldWithPath("beerName").description("Name of the beer"),
                                fieldWithPath("beerStyle").description("Style of the beer"),
                                fieldWithPath("upc").description("UPC code of the beer"),
                                fieldWithPath("price").description("Price of the beer"),
                                fieldWithPath("quantityOnHand").description("Quantity on Hand")
                        )
                    )
                 );
    }

    @Test
    void saveNewBeer() throws Exception {
        BeerDto beerDto =  getValidBeerDto();
        String beerDtoJson = objectMapper.writeValueAsString(beerDto);

        ConstrainedFields fields = new ConstrainedFields(BeerDto.class);

        ResultActions perform = mockMvc.perform(post("/api/v1/beer/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(beerDtoJson));

                perform.andExpect(status().isCreated());

                perform.andDo(document("v1/beer-new",
                        requestFields(
                                fields.withPath("id").ignored(),
                                fields.withPath("version").ignored(),
                                fields.withPath("createdDate").ignored(),
                                fields.withPath("lastModifiedDate").ignored(),
                                fields.withPath("beerName").description("Name of the beer"),
                                fields.withPath("beerStyle").description("Style of the beer"),
                                fields.withPath("upc").description("UPC code of the beer"),
                                fields.withPath("price").description("Price of the beer"),
                                fields.withPath("quantityOnHand").ignored()
                        )));
    }

    @Test
    void updateBeerById() throws Exception {
        BeerDto beerDto =  getValidBeerDto();
        String beerDtoJson = objectMapper.writeValueAsString(beerDto);

        mockMvc.perform(put("/api/v1/beer/" + UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(beerDtoJson))
                .andExpect(status().isNoContent());
    }

    BeerDto getValidBeerDto(){
        return BeerDto.builder()
                .beerName("Nice Ale")
                .beerStyle(BeerStyleEnum.ALE)
                .price(new BigDecimal("9.99"))
                .upc(123123123123L)
                .build();

    }

    private static class ConstrainedFields {

        private final ConstraintDescriptions constraintDescriptions;

        ConstrainedFields(Class<?> input) {
            this.constraintDescriptions = new ConstraintDescriptions(input);
        }

        private FieldDescriptor withPath(String path) {
            return fieldWithPath(path).attributes(key("constraints").value(StringUtils
                    .collectionToDelimitedString(this.constraintDescriptions
                            .descriptionsForProperty(path), ". ")));
        }
    }

}