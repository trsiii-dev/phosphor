import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds tour.svg, the animated picture in the README that pages through six
 * of the mock-ups. Run it from the repository's folder after changing one of
 * the six pictures:
 *
 * <pre>
 * java mockup-images/BuildTour.java
 * </pre>
 *
 * <p>An SVG shown in a README cannot run scripts or load other files, so the
 * six pictures are copied into one file as layers, and CSS shows one layer at
 * a time. Each picture gets two rewrites on the way in. Its CSS rules are
 * scoped to its layer (".v" becomes "#f3 .v" in the third layer), and its ids
 * are prefixed with the layer's name because an id must be unique in a file.
 */
public final class BuildTour {

    /** A mock-up file and the page name used in the tour's title. */
    private record Page(String file, String name) {}

    /** One picture split into its CSS and its drawing. */
    private record Frame(String css, String body) {}

    /** Same order as the TOUR list in the website's script. */
    private static final List<Page> TOUR = List.of(
            new Page("flight-page.svg", "FLIGHT / MAIN"),
            new Page("lights-page.svg", "SHIP / LIGHTS"),
            new Page("vitals-page.svg", "PILOT / VITALS"),
            new Page("figures-page.svg", "SHIP / FIGURES"),
            new Page("modes-page.svg", "FLIGHT / MODES"),
            new Page("power-page.svg", "SHIP / POWER"));

    /** The website's screen uses the same four seconds. */
    private static final int SECONDS_PER_PAGE = 4;

    /** Every mock-up is drawn at this size, and so is the tour. */
    private static final String SIZE = "viewBox=\"0 0 700 900\"";

    /**
     * An MFD with a dark screen, drawn under the layers. It shows through when
     * a layer dims at the start of its turn, so the frame and buttons stay
     * steady while the screen blinks.
     */
    private static final String EMPTY_MFD = """
            <rect width="700" height="900" fill="#1B1E22"/>
            <g stroke="#2C3138" stroke-width="22">
            <path d="M78 25H622" stroke-dasharray="64 56"/>
            <path d="M78 875H622" stroke-dasharray="64 56"/>
            <path d="M25 98V802" stroke-dasharray="64 96"/>
            <path d="M675 98V802" stroke-dasharray="64 96"/>
            </g>
            <rect x="50" y="50" width="600" height="800" rx="6" fill="#04070A"/>
            """;

    private static final Pattern ROOT_TAG = Pattern.compile("<svg\\b[^>]*>");
    private static final Pattern TITLE = Pattern.compile("<title\\b[^>]*>.*?</title>\\s*", Pattern.DOTALL);
    private static final Pattern STYLE = Pattern.compile("<style\\b[^>]*>(.*?)</style>\\s*", Pattern.DOTALL);
    private static final Pattern ID = Pattern.compile("\\bid=\"([^\"]+)\"");
    private static final Pattern CSS_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
    // Script elements, event-handler attributes, and links to anything outside the file.
    private static final Pattern ACTIVE = Pattern.compile(
            "<script|<foreignObject|\\son[a-z]+\\s*=|href\\s*=\\s*[\"'](?!#)", Pattern.CASE_INSENSITIVE);

    private BuildTour() {}

    public static void main(String[] args) throws IOException {
        Path folder = Path.of(args.length > 0 ? args[0] : "mockup-images");
        if (!Files.isDirectory(folder)) {
            System.err.println("No such folder: " + folder
                    + ". Run from the repository root: java mockup-images/BuildTour.java");
            System.exit(2);
        }
        try {
            Path target = folder.resolve("tour.svg");
            Files.writeString(target, build(folder), StandardCharsets.UTF_8);
            System.out.println("Built " + target + ": " + TOUR.size() + " pages, " + SECONDS_PER_PAGE + " s each.");
        } catch (IllegalStateException e) {
            System.err.println("Build failed: " + e.getMessage());
            System.exit(1);
        }
    }

    /** Returns the full text of tour.svg. */
    private static String build(Path folder) throws IOException {
        StringBuilder styles = new StringBuilder();
        StringBuilder layers = new StringBuilder();
        List<String> names = new ArrayList<>();
        for (int i = 0; i < TOUR.size(); i++) {
            Page page = TOUR.get(i);
            String layer = "f" + (i + 1);
            Frame frame = read(folder.resolve(page.file()), layer);
            styles.append(frame.css());
            layers.append("<g id=\"").append(layer).append("\" class=\"frame\">\n")
                    .append(frame.body().strip())
                    .append("\n</g>\n");
            names.add(page.name());
        }

        int count = TOUR.size();
        int cycle = count * SECONDS_PER_PAGE;
        // Percent of the cycle that one page is lit.
        double share = 100.0 / count;

        StringBuilder out = new StringBuilder();
        out.append("<svg xmlns=\"http://www.w3.org/2000/svg\" ").append(SIZE)
                .append(" width=\"700\" height=\"900\" role=\"img\" aria-labelledby=\"tour-title\">\n");
        out.append("<!-- Generated by BuildTour.java. Do not edit. -->\n");
        out.append("<title id=\"tour-title\">Phosphor mock-up: one screen paging through ")
                .append(String.join(", ", names))
                .append("</title>\n");

        out.append("<style>\n");
        // Layers are hidden unless the animation shows them. The first layer is
        // visible by default, so viewers that do not animate or have reduced motion
        // set see the first page instead of an empty screen.
        out.append(".frame{opacity:0;animation:tour ").append(cycle).append("s linear infinite}\n");
        out.append("#f1{opacity:1}\n");
        // Every layer runs the same animation, each delayed by one more page.
        for (int i = 1; i < count; i++) {
            out.append("#f").append(i + 1).append("{animation-delay:").append(i * SECONDS_PER_PAGE).append("s}\n");
        }
        // Visible for its share of the cycle, with a quick dip at the start like a
        // screen switching on.
        out.append(String.format(
                Locale.ROOT,
                "@keyframes tour{0%%{opacity:1}0.5%%{opacity:.3}1%%{opacity:1}%.3f%%{opacity:1}%.3f%%{opacity:0}100%%{opacity:0}}\n",
                share - 0.01,
                share));
        out.append("@media (prefers-reduced-motion:reduce){.frame{animation:none}}\n");
        out.append(styles);
        out.append("</style>\n");

        out.append(EMPTY_MFD);
        out.append(layers);
        out.append("</svg>\n");
        return out.toString();
    }

    /** Reads one picture and rewrites its CSS and ids for the given layer. */
    private static Frame read(Path file, String layer) throws IOException {
        if (!Files.isRegularFile(file)) {
            throw new IllegalStateException("missing picture " + file);
        }
        String svg = Files.readString(file, StandardCharsets.UTF_8);
        // The pictures are copied into a file that GitHub serves, so they must be plain drawings.
        if (ACTIVE.matcher(svg).find()) {
            throw new IllegalStateException(file + " contains a script, an event handler, or an external link");
        }

        // The drawing is everything between the opening <svg> tag and the last </svg>.
        Matcher root = ROOT_TAG.matcher(svg);
        int end = svg.lastIndexOf("</svg>");
        if (!root.find() || end < root.end()) {
            throw new IllegalStateException(file + " is not an SVG file");
        }
        if (!root.group().contains(SIZE)) {
            throw new IllegalStateException(file + " is not 700 by 900 (" + SIZE + ")");
        }
        // Drop the picture's title because the tour has its own.
        String body = TITLE.matcher(svg.substring(root.end(), end)).replaceFirst("");

        // Pull the style rules out of the drawing.
        StringBuilder found = new StringBuilder();
        Matcher style = STYLE.matcher(body);
        while (style.find()) {
            found.append(style.group(1)).append('\n');
        }
        body = STYLE.matcher(body).replaceAll("");
        String rules = CSS_COMMENT.matcher(found).replaceAll("");
        if (rules.contains("@")) {
            throw new IllegalStateException(file + " uses an @ rule in its CSS, which is not supported");
        }

        // Prefix every id with the layer's name, including references to it.
        Set<String> ids = new LinkedHashSet<>();
        Matcher id = ID.matcher(body);
        while (id.find()) {
            ids.add(id.group(1));
        }
        for (String name : ids) {
            String renamed = layer + "-" + name;
            body = body.replace("id=\"" + name + "\"", "id=\"" + renamed + "\"")
                    .replace("url(#" + name + ")", "url(#" + renamed + ")")
                    .replace("href=\"#" + name + "\"", "href=\"#" + renamed + "\"");
            rules = rules.replaceAll(
                    "#" + Pattern.quote(name) + "(?![\\w-])", "#" + Matcher.quoteReplacement(renamed));
        }

        // Scope each rule to this layer, so "a,b{...}" becomes "#f1 a,#f1 b{...}".
        StringBuilder css = new StringBuilder();
        for (String rule : rules.split("}")) {
            int brace = rule.indexOf('{');
            if (brace < 0) {
                continue; // whitespace after the last rule
            }
            List<String> selectors = new ArrayList<>();
            for (String selector : rule.substring(0, brace).split(",")) {
                selectors.add("#" + layer + " " + selector.strip());
            }
            css.append(String.join(",", selectors))
                    .append('{')
                    .append(rule.substring(brace + 1).strip())
                    .append("}\n");
        }
        return new Frame(css.toString(), body);
    }
}
