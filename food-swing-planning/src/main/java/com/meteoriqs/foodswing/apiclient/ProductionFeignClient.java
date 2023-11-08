package com.meteoriqs.foodswing.apiclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name="productionClient", url = "${app.production.url}")
public interface ProductionFeignClient {
    @GetMapping("/api/recipe/{id}")
    String getRecipe(@PathVariable("id") Long id);

    @PostMapping(value = "/api/createRecipe", consumes = "application/json")
    String createRecipe(@RequestBody String requestBody);
}
