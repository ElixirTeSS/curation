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
    E_LEARNING("e_Learning"),
    WORKFLOWS("workflows"),
    PROVIDERS("Providers");

    private final String name;

    CategoryEnum(String name){
        this.name = name;
    }

}