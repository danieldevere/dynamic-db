create table book (
    `id` bigint(20) not null auto_increment,
    `title` varchar(255) null default null,
    `author` varchar(255) null default null,
    primary key(`id`)
);