package org.springframework.samples.dwarf.carta;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.springframework.samples.dwarf.jugador.Jugador;
import org.springframework.samples.dwarf.tablero.Enano;
import org.springframework.samples.dwarf.tablero.Tablero;

import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@DiscriminatorValue(value = "especial")
public class CartaEspecial extends Carta {

    @Override
    public void accion5(Tablero tablero, Jugador j, Enano e) {

    }

}
