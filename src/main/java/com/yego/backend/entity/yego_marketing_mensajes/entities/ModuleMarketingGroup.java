package com.yego.backend.entity.yego_marketing_mensajes.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "module_marketing_groups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ModuleMarketingGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "group_id", unique = true, nullable = false)
    private String groupId;

    @Column(name = "subject")
    private String subject;

    @Column(name = "subject_owner")
    private String subjectOwner;

    @Column(name = "subject_time")
    private Long subjectTime;

    @Column(name = "picture_url", columnDefinition = "TEXT")
    private String pictureUrl;

    @Column(name = "size")
    private Integer size;

    @Column(name = "creation")
    private Long creation;

    @Column(name = "owner")
    private String owner;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "description_id")
    private String descriptionId;

    @Column(name = "restrict")
    private Boolean restrict;

    @Column(name = "announce")
    private Boolean announce;

    @Column(name = "is_community")
    private Boolean isCommunity;

    @Column(name = "is_community_announce")
    private Boolean isCommunityAnnounce;
}
