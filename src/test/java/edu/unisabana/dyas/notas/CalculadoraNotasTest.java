package edu.unisabana.dyas.notas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CalculadoraNotasTest {

    private final CalculadoraNotas calculadora = new CalculadoraNotas();

    @ParameterizedTest(name = "{0}, {1}, {2} -> {3}")
    @CsvSource({
            "5.0, 5.0, 5.0, 4.0",
            "0.0, 0.0, 0.0, 0.0",
            "3.0, 3.0, 3.0, 3.0",
            "4.0, 3.5, 2.0, 3.1", // 3.05 se redondea hacia arriba
            "2.0, 2.5, 3.5, 2.8"
    })
    void calculaLaDefinitivaPonderada(double c1, double c2, double c3, double esperada) {
        assertEquals(esperada, calculadora.calcularDefinitiva(c1, c2, c3));
    }

    @Test
    void apruebaConTresOMas() {
        assertTrue(calculadora.aprueba(3.0));
        assertTrue(calculadora.aprueba(4.2));
    }

    @Test
    void repruebaConMenosDeTres() {
        assertFalse(calculadora.aprueba(2.9));
    }

    @ParameterizedTest
    @CsvSource({"-0.1, 3.0, 3.0", "3.0, 5.1, 3.0", "3.0, 3.0, 7.0"})
    void rechazaNotasFueraDeRango(double c1, double c2, double c3) {
        assertThrows(IllegalArgumentException.class, () -> calculadora.calcularDefinitiva(c1, c2, c3));
    }

    @Test
    void rechazaDefinitivaFueraDeRango() {
        assertThrows(IllegalArgumentException.class, () -> calculadora.aprueba(5.5));
    }
}
