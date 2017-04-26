package kis.lotterybot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class DataController {
    @RequestMapping(value = "/hello", method = RequestMethod.GET)
    @ResponseBody
    public String hello(Model model) {
        return "hello mvc";
    }

    @RequestMapping(value = "/uploadMembersExec", method = RequestMethod.POST)
    @ResponseBody
    public String uploadMembersExec(@RequestParam("datafile") MultipartFile input) throws IOException {
        Path parent = Main.MEMBER_PATH.getParent();
        if (!Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        Files.copy(input.getInputStream(), Main.MEMBER_PATH, StandardCopyOption.REPLACE_EXISTING);
        return "uploaded";
    }
}
