/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.dwarf.user;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import javax.validation.Valid;

import org.h2.expression.analysis.PartitionData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.samples.dwarf.jugador.Jugador;
import org.springframework.samples.dwarf.jugador.JugadorService;
import org.springframework.samples.dwarf.logro.Logro;
import org.springframework.samples.dwarf.logro.LogroService;
import org.springframework.samples.dwarf.tablero.Tablero;
import org.springframework.samples.dwarf.tablero.TableroService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Arjen Poutsma
 * @author Michael Isvy
 */
@Controller
public class UserController {

    private static final String VIEWS_OWNER_CREATE_FORM = "users/createOwnerForm";
    private static final String view_user = "users/showUser";
    private static final String VIEW_USERS_LIST = "users/usersList";

    private final JugadorService ownerService;
    private final UserService userService;
    private final LogroService logroService;
    private final InvitacionAmistadService invitacionAmistadService;
    private final AuthoritiesService authoritiesService;
    private final EstadisticaService estadisticaService;
    private final TableroService taservice;
    private final JugadorService jService;

    @Autowired
    public UserController(JugadorService clinicService, UserService userService, LogroService logroService,
            InvitacionAmistadService invitacionAmistadService, AuthoritiesService authoritiesService,
            EstadisticaService estadisticaService, TableroService taservice, JugadorService jService) {
        this.ownerService = clinicService;
        this.userService = userService;
        this.logroService = logroService;
        this.invitacionAmistadService = invitacionAmistadService;
        this.authoritiesService = authoritiesService;
        this.estadisticaService = estadisticaService;
        this.taservice = taservice;
        this.jService = jService;
    }

    @InitBinder
    public void setAllowedFields(WebDataBinder dataBinder) {
        dataBinder.setDisallowedFields("id");
    }

    @GetMapping("/users")
    public String parteDeUsuarios(Map<String, Object> model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        org.springframework.security.core.userdetails.User currentUser = (org.springframework.security.core.userdetails.User) authentication
                .getPrincipal();
        List<User> totalUsuarios = new ArrayList<>();
        Map<User, List<Integer>> totalJugadores = new HashMap<>();
        Map<User, Integer> acum = new HashMap<>();
        for (User u : userService.findAll()) {
            totalUsuarios.add(u);

            List<Integer> ls = new ArrayList<>();
            totalJugadores.put(u, ls);
            for (Jugador j : jService.findJugadorUser(u.getUsername())) {
                totalJugadores.get(u).add(j.getPosicionFinal());

            }

            for (int i = 0; i < totalJugadores.size(); i++) {
                List<Integer> aux = totalJugadores.get(u);
                Integer total = 0;
                if (!aux.isEmpty()) {
                    total = aux.stream().reduce((t, b) -> t + b).get();
                }

                acum.put(u, total);
            }
        }

        model.put("perfil", currentUser.getUsername());
        model.put("usuarios", totalUsuarios);
        model.put("puntuacion", acum);
        return "users/welcomecopy";
    }

    @GetMapping(value = "/user")
    public String usersList(Map<String, Object> model, @RequestParam Integer page) {
        List<User> usuarios = userService.findAll();

        final int PAGE_SIZE = 5;
        int pageNumber = 0;

        if (usuarios.size() % PAGE_SIZE != 0) {
            pageNumber = (usuarios.size() / PAGE_SIZE) + 1;
        } else {
            pageNumber = usuarios.size() / PAGE_SIZE;
        }

        List<List<User>> partition = new ArrayList<>();
        for (int i = 0; i < pageNumber; i++) {
            partition.add(new ArrayList<>());
        }

        int startIndex = 0;
        int finishIndex = PAGE_SIZE;
        for (int i = 0; i < partition.size(); i++) {
            if (finishIndex > usuarios.size()) {
                partition.set(i, usuarios.subList(startIndex, usuarios.size()));
                break;
            }
            partition.set(i, usuarios.subList(startIndex, finishIndex));
            startIndex += PAGE_SIZE;
            finishIndex += PAGE_SIZE;
        }

        List<User> totalUsuarios = new ArrayList<>();
        Map<User, List<Integer>> totalJugadores = new HashMap<>();
        Map<User, Integer> acum = new HashMap<>();
        for (User u : userService.findAll()) {
            totalUsuarios.add(u);

            List<Integer> ls = new ArrayList<>();
            totalJugadores.put(u, ls);
            for (Jugador j : jService.findJugadorUser(u.getUsername())) {
                totalJugadores.get(u).add(j.getPosicionFinal());

            }

            for (int i = 0; i < totalJugadores.size(); i++) {
                List<Integer> aux = totalJugadores.get(u);
                Integer total = 0;
                if (!aux.isEmpty()) {
                    total = aux.stream().reduce((t, b) -> t + b).get();
                }

                acum.put(u, total);
            }
        }
        model.put("puntuacion", acum);
        model.put("usuarios", partition.get(page));
        model.put("paginaActual", page);
        model.put("paginas", IntStream.rangeClosed(0, partition.size() - 1)
                .boxed().toList());
        return VIEW_USERS_LIST;
    }

    @GetMapping(value = "/user/find")
    public String initCreationForm(Map<String, Object> model) {
        User user = new User();
        model.put("user", user);
        return "users/findUsers";
    }

    @PostMapping(value = "/user/find")
    public String processCreationForm(Map<String, Object> model, @Valid User user, BindingResult result) {
        if (result.hasErrors()) {
            return "users/findUsers";
        } else {
            // creating owner, user, and authority
            model.put("usuarios", userService.findUserByString(user.username));
            return VIEW_USERS_LIST;
        }
    }

    @PostMapping(value = "/users/friend")
    public String processAddFriendForm(Map<String, Object> model, @Valid User user, BindingResult result) {
        if (result.hasErrors()) {
            org.springframework.security.core.userdetails.User currentUser = (org.springframework.security.core.userdetails.User) SecurityContextHolder
                    .getContext().getAuthentication()
                    .getPrincipal();
            return "redirect:/users/" + currentUser.getUsername();
        } else {
            // creating owner, user, and authority

            String enviaUsername = "";
            User recibe = userService.findUser(user.username).get();
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null) {
                if (authentication.isAuthenticated()) {
                    org.springframework.security.core.userdetails.User currentUser = (org.springframework.security.core.userdetails.User) authentication
                            .getPrincipal();

                    enviaUsername = currentUser.getUsername();

                }
            }
            // si no encuentra al usuario
            if (!userService.findAll().contains(userService.findUser(user.username).get())) {
                return "redirect:/users/" + enviaUsername;
            }

            // amiga que ya esta
            if (invitacionAmistadService.findFriendsUser(userService.findUser(enviaUsername).get())
                    .contains(user.username)) {

                return "redirect:/users/" + enviaUsername;
            }
            InvitacionAmistad invitacionAmistad = new InvitacionAmistad();
            invitacionAmistad.setUserenvia(userService.findUser(enviaUsername).get());
            invitacionAmistad.setUserrecibe(recibe);

            invitacionAmistadService.saveInvitacionAmistad(invitacionAmistad);
            model.put("usuarios", invitacionAmistadService.findFriendsUser(userService.findUser(enviaUsername).get()));
            return "redirect:/users/" + enviaUsername;
        }
    }

    @GetMapping(value = "/users/{userid}")
    public String showUser(@PathVariable("userid") String id, Map<String, Object> model) {
        User usuario = userService.findUser(id).get();
        List<Jugador> jugadores = ownerService.findJugadorUser(id);

        List<Logro> logrosAll = logroService.findAll();
        List<Logro> logrosCumplidos = new ArrayList<>();

        for (Logro logro : logrosAll) {
            if (logro.getTipo().getName().equals("partidas_ganadas")) {
                if (usuario.getEstadistica().getPartidasGanadas() >= logro.getRequisito())
                    logrosCumplidos.add(logro);
            }
            if (logro.getTipo().getName().equals("recursos_conseguidos")) {

                Integer hierro = 0;
                Integer acero = 0;
                Integer objetos = 0;
                Integer medallas = 0;
                Integer oro = 0;
                for (Jugador j : jugadores) {
                    hierro += j.getHierro();
                    acero += j.getAcero();
                    objetos += j.getObjeto();
                    medallas += j.getMedalla();
                    oro += j.getOro();
                }

                if (logro.getName().contains("hierro") && hierro >= logro.getRequisito())
                    logrosCumplidos.add(logro);
                if (logro.getName().contains("acero") && acero >= logro.getRequisito())
                    logrosCumplidos.add(logro);
                if (logro.getName().contains("objetos") && objetos >= logro.getRequisito())
                    logrosCumplidos.add(logro);
                if (logro.getName().contains("medallas") && medallas >= logro.getRequisito())
                    logrosCumplidos.add(logro);
                if (logro.getName().contains("oro") && oro >= logro.getRequisito())
                    logrosCumplidos.add(logro);
            }

        }

        List<User> usuarios = invitacionAmistadService.findFriends(usuario).stream()
                .map(invitacion -> invitacion.getUserrecibe()).toList();

        model.put("usuarios", usuarios);
        model.put("imagen", usuario.imgperfil);
        model.put("user", new User());
        model.put("usuario", usuario);
        model.put("jugadores", jugadores);
        model.put("logros", logrosCumplidos);

        final Integer PARTIDAS_MOSTRADAS = 7;
        model.put("partidas", taservice.findLastNGamesByUser(usuario, PARTIDAS_MOSTRADAS));

        return view_user;
    }

    @GetMapping("usersnew")
    public String createNewUser(Map<String, Object> model) {
        User user = new User();
        model.put("user", user);
        return "users/creatForm";
    }

    @PostMapping("usersnew")
    public String createNewUser(@Valid User user, BindingResult result, RedirectAttributes redatt) {
        if (result.hasErrors()) {
            return "redirect:/users/new";
        } else {

            if (userService.findUser(user.getUsername()).isPresent()) {
                redatt.addFlashAttribute("mensaje", "Ya existe un usuario con este nombre");
                return "redirect:/usersnew";
            }

            userService.saveUser(user);
            Estadistica estats = new Estadistica();
            estats.setUsuario(user);
            estats.setPartidasGanadas(0);
            estats.setPartidasPerdidas(0);
            estadisticaService.saveEstadistica(estats);
            Authorities authority = new Authorities();
            authority.setAuthority("jugador");
            authority.setUser(user);
            authoritiesService.saveAuthorities(authority);
            return "redirect:/";
        }
    }

    @GetMapping("users/mod")
    public String modifyUser(Map<String, Object> model) {
        User user = new User();
        model.put("user", user);
        return "users/creatForm";
    }

    @PostMapping("users/mod")
    public String modifyUser(@Valid User user, BindingResult result, RedirectAttributes redatt) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication.isAuthenticated()) {
            org.springframework.security.core.userdetails.User currentUser = (org.springframework.security.core.userdetails.User) authentication
                    .getPrincipal();
            if (currentUser.getUsername().equals(user.getUsername())) {
                userService.saveUser(user);
                Authorities authority = new Authorities();
                authority.setAuthority("jugador");
                authority.setUser(user);
                authoritiesService.saveAuthorities(authority);
                return "redirect:/";
            }

            redatt.addFlashAttribute("mensaje", "No eres propietario de este usuario");
            return "redirect:/users/mod";

        }
        return "redirect:/";
    }
}
