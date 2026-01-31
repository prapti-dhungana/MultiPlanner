package com.multiplanner.api.service;
import java.util.List;
import org.springframework.stereotype.Service;
import com.multiplanner.api.model.Station;

@Service
public class StationService {

    public List<Station> searchStations(String query){
        String q = query == null ? "" : query.toLowerCase();

        //Temp stub data
        List<Station> all =  List.of(
            new Station("LST", "london Liverpool Street"),
            new Station("KGX", "London Kings Cross"),
            new Station("LBG", "London Bridge")
        ); 

        return all.stream()
                    .filter(s -> s.getName().toLowerCase().contains(q) || s.getCode().toLowerCase().contains(q))
                    .toList();
    }
      
}
