package cn.e3mall.itemdisplay.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ItemdisplayController {

    @RequestMapping("/item/{itemId}")
    private String showItemInfo(@PathVariable Long itemId){
        return itemId+"";
    }
}
