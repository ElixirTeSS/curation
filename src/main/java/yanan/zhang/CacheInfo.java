package yanan.zhang;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author Yanan Zhang
 **/
@Data
@AllArgsConstructor
public class CacheInfo {

    private String url;
    private int httpStatus;
    private String reasonPhrase;
    private Integer page;
    private String detailLink;
    private String deadLinkTitle;

}