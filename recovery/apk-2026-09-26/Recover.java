import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.Predicate;
import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.DecompilationMode;

class Recover {
    public static void main(String[] args) throws Exception {
        if (args.length < 2 || args.length > 4) throw new IllegalArgumentException("APK, output, optional class list and mode required");
        JadxArgs config = new JadxArgs();
        config.setInputFile(new File(args[0]));
        config.setOutDir(new File(args[1]));
        config.setSkipResources(true);
        config.setShowInconsistentCode(true);
        config.setReplaceConsts(false);
        config.setUseKotlinMethodsForVarNames(JadxArgs.UseKotlinMethodsForVarNames.DISABLE);
        config.setThreadsCount(3);
        Predicate<String> selected = name -> name.startsWith("vea.raceremote.")
                && !name.equals("vea.raceremote.R") && !name.startsWith("vea.raceremote.R$")
                && (args.length < 3 || Arrays.asList(args[2].split(",")).contains(name));
        config.setClassFilter(selected);
        if (args.length == 4) config.setDecompilationMode(DecompilationMode.valueOf(args[3]));
        try (JadxDecompiler decompiler = new JadxDecompiler(config)) {
            decompiler.load();
            long count = decompiler.getClasses().stream()
                    .filter(c -> selected.test(c.getFullName())).count();
            if (count == 0) throw new IllegalStateException("No RaceRemote classes found");
            decompiler.saveSources();
            if (args.length == 2) {
                for (var resource : decompiler.getResources()) {
                    if (resource.getOriginalName().equals("AndroidManifest.xml")) {
                        Files.writeString(Path.of(args[1], "AndroidManifest.xml"),
                                resource.loadContent().getText().getCodeStr());
                    }
                }
            }
            System.out.println("RECOVERY classes=" + count + " errors=" + decompiler.getErrorsCount());
        }
    }
}
