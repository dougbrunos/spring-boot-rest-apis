package br.com.douglas.controllers;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/math")
public class MathController {

    @RequestMapping("/sum/{numberOne}/{numberTwo}")
    public Double sum(
            @PathVariable("numberOne") String numberOne,
            @PathVariable("numberTwo") String numberTwo
    ) throws IllegalArgumentException {
        if(!isNumeric(numberOne) || !isNumeric(numberTwo)) throw new IllegalArgumentException();
        return convertToDouble(numberOne) + convertToDouble(numberTwo);
    }

    private Double convertToDouble(String strNumber) throws IllegalArgumentException {
        if (strNumber == null || strNumber.isEmpty()) throw new IllegalArgumentException();
        String number = strNumber.replace(",", ".");
        return Double.parseDouble(number);
    }

    private boolean isNumeric(String strNumber) {
        if (strNumber == null || strNumber.isEmpty()) return false;
        String number = strNumber.replace(",", ".");
        return number.matches("[-+]?[0-9]*\\.?[0-9]+");
    }

}
