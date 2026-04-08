---                                                                                                                                                                                   
Migration Spring Shell → Tamboui

Résumé

Remplace le REPL Spring Shell par une interface TUI plein-écran : panneau d'historique scrollable en haut, champ de saisie en bas.
                                                                                                     
---                                                                                                                                                                                   
Étape 1 — pom.xml

Supprimer :
- Propriétés <spring-shell.version> et <jline.version>
- La BOM spring-shell-dependencies dans <dependencyManagement>
- Les dépendances spring-shell-starter, spring-shell-jline, org.jline:jline

Ajouter :

Propriété :                                                                                                                                                                           
<tamboui.version>0.2.0-SNAPSHOT</tamboui.version>

Repository Sonatype Snapshots dans <repositories> :                                                                                                                                   
<repository>                                                                                                                                                                          
<id>sonatype-snapshots</id>                                                                                                                                                       
<url>https://central.sonatype.com/repository/maven-snapshots/</url>                                                                                                               
<snapshots><enabled>true</enabled></snapshots>                                                                                                                                  
<releases><enabled>false</enabled></releases>                                                                                                                                     
</repository>

BOM dans <dependencyManagement> :                                                                                                                                                   
<dependency>                                                                                                                                                                          
<groupId>dev.tamboui</groupId>
<artifactId>tamboui-bom</artifactId>                                                                                                                                              
<version>${tamboui.version}</version>                                                                                                                                           
<type>pom</type>                                                                                                                                                                  
<scope>import</scope>          
</dependency>

Dépendances :                                                                                      
<dependency>                                                                                                                                                                          
<groupId>dev.tamboui</groupId>
<artifactId>tamboui-toolkit</artifactId>                                                                                                                                          
</dependency>                                                                                                                                                                       
<dependency>                                                                                                                                                                          
<groupId>dev.tamboui</groupId>
<artifactId>tamboui-jline3-backend</artifactId>                                                                                                                                   
</dependency>                                                                                                                                                                       
<dependency>                                                                                                                                                                          
<groupId>dev.tamboui</groupId>
<artifactId>tamboui-processor</artifactId>                                                                                                                                        
<scope>provided</scope>                                                                                                                                                         
</dependency>

Plugin maven-compiler-plugin (pour le native GraalVM) :                                                                                                                               
<plugin>                                                                                                                                                                              
<groupId>org.apache.maven.plugins</groupId>                                                    
<artifactId>maven-compiler-plugin</artifactId>                                                                                                                                    
<configuration>                                                                                                                                                                 
<annotationProcessorPaths>                                                                                                                                                    
<path>                 
<groupId>dev.tamboui</groupId>                                                                                                                                        
<artifactId>tamboui-processor</artifactId>                                                                                                                          
<version>${tamboui.version}</version>                                              
</path>                                                                                                                                                                   
</annotationProcessorPaths>
</configuration>                                                                                                                                                                  
</plugin>
                                                                                                     
---                                
Étape 2 — Supprimer ShellCommands.java

rm src/main/java/com/ggardet/codingagent/shell/ShellCommands.java                                                                                                                     
rmdir src/main/java/com/ggardet/codingagent/shell
                                                                                                     
---                                                                                                                                                                                   
Étape 3 — Créer CodingAgentTui.java

Créer src/main/java/com/ggardet/codingagent/tui/CodingAgentTui.java :

package com.ggardet.codingagent.tui;

import com.ggardet.codingagent.service.AgentService;
import dev.tamboui.toolkit.app.ToolkitApp;                                                                                                                                            
import dev.tamboui.toolkit.element.Element;                                                                                                                                           
import dev.tamboui.toolkit.elements.ListElement;                                                   
import dev.tamboui.toolkit.elements.SpinnerElement;                                                                                                                                   
import dev.tamboui.toolkit.elements.TextInputElement;                                                                                                                               
import dev.tamboui.tui.TuiConfig;                                                                                                                                                     
import dev.tamboui.widgets.input.TextInputState;                                                                                                                                    
import dev.tamboui.widgets.spinner.SpinnerStyle;                                                                                                                                      
import org.springframework.stereotype.Component;

import java.time.Duration;                                                                                                                                                            
import java.util.List;                                                                             
import java.util.concurrent.CopyOnWriteArrayList;

import static dev.tamboui.toolkit.Toolkit.column;                                                                                                                                     
import static dev.tamboui.toolkit.Toolkit.dock;
import static dev.tamboui.toolkit.Toolkit.length;                                                                                                                                     
import static dev.tamboui.toolkit.Toolkit.panel;                                                                                                                                      
import static dev.tamboui.toolkit.Toolkit.spinner;                                                 
import static dev.tamboui.toolkit.Toolkit.text;                                                                                                                                       
import static dev.tamboui.toolkit.Toolkit.textInput;                                                                                                                                
import static dev.tamboui.tui.event.EventResult.HANDLED;                                           
import static dev.tamboui.tui.event.EventResult.UNHANDLED;

@Component                                                                                                                                                                            
public final class CodingAgentTui extends ToolkitApp {                                                                                                                              
private static final int BOTTOM_HEIGHT = 5;                                                                                                                                       
private static final String HINTS = " Enter: send  Ctrl+L: clear  Ctrl+C: quit";

      private final AgentService agentService;                                                                                                                                          
      private final TextInputState inputState = new TextInputState();                                                                                                                   
      private final CopyOnWriteArrayList<String> messages = new CopyOnWriteArrayList<>();                                                                                             
      private final ListElement<?> historyList = new ListElement<>();                                                                                                                   
      private final SpinnerElement loadingSpinner = spinner(SpinnerStyle.DOTS, "Thinking...");
      private final TextInputElement inputField = textInput(inputState)                                                                                                                 
              .placeholder("Type a message and press Enter...")                                                                                                                         
              .focusable()                                                                                                                                                              
              .rounded()                                                                                                                                                                
              .onSubmit(this::sendMessage);                                                                                                                                           
      private volatile boolean loading = false;                                                                                                                                         
                                     
      public CodingAgentTui(final AgentService agentService) {                                                                                                                          
          this.agentService = agentService;                                                                                                                                           
      }                                                                                                                                                                                 
                                                                                                                                                                                      
      @Override                                                                                      
      protected TuiConfig configure() {
          return TuiConfig.builder()
                  .tickRate(Duration.ofMillis(100))                                                                                                                                     
                  .build();
      }                                                                                                                                                                                 
                                                                                                                                                                                      
      @Override                                                                                      
      protected Element render() {   
          historyList.items(List.copyOf(messages));
          final var activeInput = loading ? loadingSpinner : inputField;                                                                                                                
          final var bottom = panel(                                                                                                                                                     
                  column(activeInput, text(HINTS).dim()).spacing(0)                                                                                                                     
          ).borderless();                                                                                                                                                               
          return dock()                                                                                                                                                               
                  .center(historyList.displayOnly().scrollToEnd())                                                                                                                      
                  .bottom(bottom, length(BOTTOM_HEIGHT))                                                                                                                                
                  .focusable()                                                                       
                  .onKeyEvent(event -> {                                                                                                                                                
                      if (event.hasCtrl() && event.isChar('l')) {                                                                                                                       
                          clearConversation();                                                       
                          return HANDLED;                                                                                                                                               
                      }                                                                                                                                                               
                      return UNHANDLED;                                                                                                                                                 
                  });                                                                                                                                                                 
      }                                                                                              
                                     
      @Override
      protected void onStart() {
          messages.add("Coding Agent ready. Type a message below.");
      }                                                                                                                                                                                 
   
      private void sendMessage() {                                                                                                                                                      
          final var message = inputState.text().trim();                                                                                                                               
          if (message.isEmpty()) {                                                                                                                                                      
              return;                                                                                                                                                                 
          }                                                                                          
          inputState.clear();        
          messages.add("> " + message);
          loading = true;                                                                                                                                                               
          Thread.ofVirtual().start(() -> {
              final var response = agentService.chat(message);                                                                                                                          
              messages.add("  " + response);                                                                                                                                            
              loading = false;                                                                       
          });                                                                                                                                                                           
      }                                                                                             
                                                                                                     
      private void clearConversation() {
          agentService.clearMemory();
          messages.clear();                                                                                                                                                             
      }
}
                                                                                                    
---                                                                                                
Étape 4 — Modifier CodingAgentApplication.java

package com.ggardet.codingagent;

import com.ggardet.codingagent.config.AgentToolsRuntimeHints;                                                                                                                         
import com.ggardet.codingagent.tui.CodingAgentTui;                                                 
import org.springframework.boot.ApplicationArguments;                                                                                                                                 
import org.springframework.boot.ApplicationRunner;                                                                                                                                    
import org.springframework.boot.SpringApplication;                                                 
import org.springframework.boot.autoconfigure.SpringBootApplication;                                                                                                                  
import org.springframework.context.annotation.ImportRuntimeHints;

@ImportRuntimeHints(AgentToolsRuntimeHints.class)                                                                                                                                     
@SpringBootApplication                                                                            
public class CodingAgentApplication implements ApplicationRunner {                                                                                                                    
private final CodingAgentTui tui;

      public CodingAgentApplication(final CodingAgentTui tui) {                                                                                                                         
          this.tui = tui;                                                                            
      }                                                                                                                                                                                 
                                                                                                    
      static void main(final String[] args) {                                                        
          SpringApplication.run(CodingAgentApplication.class, args);
      }                                                                                                                                                                                 
   
      @Override                                                                                                                                                                         
      public void run(final ApplicationArguments args) throws Exception {                           
          tui.run();                                                                                 
          System.exit(0);            
      }
}

  ---                                                                                                                                                                                   
Points d'attention lors de l'exécution

Tamboui étant en 0.2.0-SNAPSHOT, certains noms d'imports exacts peuvent différer une fois les artefacts résolus. Après mvn compile, corrige si besoin :

┌──────────────────────────────────────────┬────────────────────────────────────────────┐                                                                                             
│               Incertitude                │           Alternative si erreur            │                                                                                             
├──────────────────────────────────────────┼────────────────────────────────────────────┤                                                                                             
│ dev.tamboui.widgets.spinner.SpinnerStyle │ Cherche SpinnerStyle dans les jars résolus │          
├──────────────────────────────────────────┼────────────────────────────────────────────┤          
│ new ListElement<>() (no-arg)             │ Remplace par list() factory de Toolkit     │                                                                                             
├──────────────────────────────────────────┼────────────────────────────────────────────┤                                                                                             
│ panel().borderless()                     │ Retire l'appel .borderless()               │                                                                                             
├──────────────────────────────────────────┼────────────────────────────────────────────┤                                                                                             
│ column().spacing(0)                      │ Retire l'appel .spacing(0)                 │          
└──────────────────────────────────────────┴────────────────────────────────────────────┘

Vérification

mvn dependency:resolve          # Tamboui snapshots se résolvent                                                                                                                      
mvn compile                     # Ajuster les imports si erreurs                                                                                                                      
mvn spring-boot:run             # Lancer le TUI interactivement                                    
mvn -Pnative native:compile     # Native image avec tamboui-processor
