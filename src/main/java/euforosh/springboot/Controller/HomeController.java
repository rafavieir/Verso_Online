package euforosh.springboot.Controller;

import org.json.JSONException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestTemplate;

import org.json.JSONArray;
import org.json.JSONObject;

@Controller
public class HomeController {

    private static final String RANDOM_API_URL = "https://bible-api.com/?random=chapter+verse:&translation=almeida";

    @GetMapping("/")
    public String home(Model model) {
        RestTemplate restTemplate = new RestTemplate();
        String response = restTemplate.getForObject(RANDOM_API_URL, String.class);

        // Extrai os dados necessários da resposta
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(response);
        } catch (JSONException e) {
            throw new RuntimeException("Erro ao criar objeto JSON a partir da resposta da API", e);
        }

        // Verifica se o valor da chave "reference" existe e está no formato desejado "2 Samuel 13"
        String reference = jsonObject.optString("reference");
        if (reference.isEmpty()) {
            throw new RuntimeException("Referência ausente na resposta da API");
        }

        // Divide a string de referência pelo primeiro espaço em branco para separar o nome do livro e o número do capítulo
        int spaceIndex = reference.indexOf(' ');
        if (spaceIndex == -1 || spaceIndex == reference.length() - 1) {
            throw new RuntimeException("Formato de referência inválido: " + reference);
        }

        String livro = reference.substring(0, spaceIndex); // O nome do livro é do início até o primeiro espaço em branco
        String capituloStr = reference.substring(spaceIndex + 1); // O número do capítulo é tudo após o primeiro espaço em branco

        // Remove o versículo, se presente
        int colonIndex = capituloStr.indexOf(':');
        if (colonIndex != -1) {
            capituloStr = capituloStr.substring(0, colonIndex);
        }

        int capitulo;
        try {
            capitulo = Integer.parseInt(capituloStr);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Número do capítulo inválido: " + capituloStr);
        }

        // Verifica se o número do capítulo é válido
        if (capitulo <= 0) {
            throw new RuntimeException("Número do capítulo inválido: " + capitulo);
        }

        // Constrói a segunda URL para obter o capítulo completo
        String chapterApiUrl = "https://bible-api.com/" + livro + capitulo + "?translation=almeida";

        // Faz a segunda requisição para obter o capítulo completo
        String chapterResponse = restTemplate.getForObject(chapterApiUrl, String.class);

        // Extrai os versículos do capítulo completo
        JSONObject chapterObject;
        try {
            chapterObject = new JSONObject(chapterResponse);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        JSONArray versesArray = chapterObject.optJSONArray("verses");
        if (versesArray == null || versesArray.length() == 0) {
            throw new RuntimeException("Array de versículos vazio ou ausente na resposta da API");
        }

        String[] verses = new String[versesArray.length()];
        for (int i = 0; i < versesArray.length(); i++) {
            try {
                verses[i] = versesArray.getJSONObject(i).getString("text");
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }

        // Adiciona os versículos ao modelo
        model.addAttribute("livro", livro);
        model.addAttribute("capitulo", capitulo);
        model.addAttribute("verses", verses);
        return "index";
    }
}
