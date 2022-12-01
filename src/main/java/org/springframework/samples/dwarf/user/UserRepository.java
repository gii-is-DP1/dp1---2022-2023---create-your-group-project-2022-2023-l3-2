package org.springframework.samples.dwarf.user;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<User, String> {

    // @Query("SELECT invitacionamistad.idrecibe FROM InvitacionAmistad
    // invitacionamistad WHERE invitacionamistad.idenvia=:idenvia")
    // List<User> findByIdEnvia(Integer idenvia);

}
