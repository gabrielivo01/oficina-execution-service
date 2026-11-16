package io.github.gabrielivo.oficina.execution.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.data.repository.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

@AnalyzeClasses(
        packages = "io.github.gabrielivo.oficina.execution",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArquiteturaTest {

    private static final String ROOT = "io.github.gabrielivo.oficina.execution";
    private static final String DOMAIN = ROOT + ".domain..";
    private static final String APPLICATION = ROOT + ".application..";
    private static final String PRESENTATION = ROOT + ".presentation..";
    private static final String INFRASTRUCTURE = ROOT + ".infrastructure..";

    @ArchTest
    static final ArchRule dominioIsolado = noClasses()
            .that()
            .resideInAPackage(DOMAIN)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(APPLICATION, PRESENTATION, INFRASTRUCTURE);

    @ArchTest
    static final ArchRule applicationSemDependenciasExternas = noClasses()
            .that()
            .resideInAPackage(APPLICATION)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(PRESENTATION, INFRASTRUCTURE);

    @ArchTest
    static final ArchRule controllersNaoAcessamRepositories = noClasses()
            .that()
            .resideInAPackage(PRESENTATION)
            .should()
            .dependOnClassesThat()
            .areAssignableTo(Repository.class);

    @ArchTest
    static final ArchRule servicesEmApplication =
            classes().that().areAnnotatedWith(Service.class).should().resideInAPackage(APPLICATION);

    @ArchTest
    static final ArchRule controllersEmPresentation =
            classes().that().areAnnotatedWith(RestController.class).should().resideInAPackage(PRESENTATION);

    @ArchTest
    static final ArchRule transacoesSomenteNaAplicacao = noMethods()
            .that()
            .areDeclaredInClassesThat()
            .resideOutsideOfPackage(APPLICATION)
            .should()
            .beAnnotatedWith(Transactional.class);

    @ArchTest
    static final ArchRule modulosSemCiclos =
            slices().matching(ROOT + ".domain.(*)..").should().beFreeOfCycles();
}
