package yanan.zhang;

import lombok.Data;

/**
 * @author Yanan Zhang
 */
@Data
public class HttpResult {

    private boolean success;
    private int httpStatus;
    private String reasonPhrase;
    private String httpContent;

}
