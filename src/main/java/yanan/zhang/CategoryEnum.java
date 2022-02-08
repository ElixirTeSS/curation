package yanan.zhang;

import lombok.Getter;

/**
 * @author Yanan Zhang
 **/
@Getter
public enum CategoryEnum {

    /**
     * Categories
     */
    EVENTS("Events"),
    MATERIALS("Materials"),
    E_LEARNING("E_Learning"),
    WORKFLOWS("Workflows");

    private final String name;

    CategoryEnum(String name){
        this.name = name;
    }
}